package dentalbackend;

import dentalbackend.common.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoleChangeIntegrationTest {

    // Testcontainers: MariaDB and Redis
    @Container
    public static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.11.3")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    public static final GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getFirstMappedPort());

        // disable flyway in tests (we rely on Hibernate ddl-auto=update)
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void roleChangeShouldInvalidateTokens() throws Exception {
        // 1) Register test user
        Map<String, Object> registerReq = Map.of(
                "username", "int-test-user",
                "email", "int-user@example.com",
                "password", "TestPass123!",
                "captchaToken", "dummy"
        );
        ResponseEntity<ApiResponse> regResp = restTemplate.postForEntity("/api/auth/register", registerReq, ApiResponse.class);
        Assertions.assertTrue(regResp.getStatusCode().is2xxSuccessful(), "Register should succeed");

        // 2) Login as admin (bootstrap DataInitializer should create admin)
        Map<String, String> adminLogin = Map.of(
                "usernameOrEmail", "admin",
                "password", "ChangeMe123!"
        );
        ResponseEntity<Map> adminLoginResp = restTemplate.postForEntity("/api/auth/login", adminLogin, Map.class);
        Assertions.assertEquals(HttpStatus.OK, adminLoginResp.getStatusCode(), "Admin login must succeed (bootstrap admin)");
        Map adminBody = adminLoginResp.getBody();
        Assertions.assertNotNull(adminBody);
        Map adminData = (Map) adminBody.get("data");
        String adminAccess = (String) adminData.get("accessToken");
        Assertions.assertNotNull(adminAccess);

        // 3) Login as test user
        Map<String, String> userLogin = Map.of(
                "usernameOrEmail", "int-test-user",
                "password", "TestPass123!"
        );
        ResponseEntity<Map> userLoginResp = restTemplate.postForEntity("/api/auth/login", userLogin, Map.class);
        Assertions.assertEquals(HttpStatus.OK, userLoginResp.getStatusCode(), "User login must succeed");
        Map userBody = userLoginResp.getBody();
        Assertions.assertNotNull(userBody);
        Map userData = (Map) userBody.get("data");
        String userAccess = (String) userData.get("accessToken");
        String userRefresh = (String) userData.get("refreshToken");
        Assertions.assertNotNull(userAccess);
        Assertions.assertNotNull(userRefresh);

        // 4) Use user's access token to call /api/users/me (should succeed)
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccess);
        HttpEntity<Void> userReq = new HttpEntity<>(userHeaders);
        ResponseEntity<Map> meResp = restTemplate.exchange("/api/users/me", HttpMethod.GET, userReq, Map.class);
        Assertions.assertEquals(HttpStatus.OK, meResp.getStatusCode());

        // 5) As admin, change test user's role to DENTIST
        Map meBody = meResp.getBody();
        Assertions.assertNotNull(meBody);
        Map meData = (Map) meBody.get("data");
        Integer userIdInt = (Integer) meData.get("id");
        Assertions.assertNotNull(userIdInt);
        long userId = userIdInt.longValue();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccess);
        HttpEntity<Void> adminReq = new HttpEntity<>(adminHeaders);

        ResponseEntity<Map> changeResp = restTemplate.exchange(
                "/api/users/" + userId + "/role?role=DENTIST",
                HttpMethod.PATCH,
                adminReq,
                Map.class
        );
        Assertions.assertEquals(HttpStatus.OK, changeResp.getStatusCode(), "Admin role change must return OK");

        // 6) Old access token should now be rejected when calling protected endpoint
        ResponseEntity<Map> meAfterResp = restTemplate.exchange("/api/users/me", HttpMethod.GET, userReq, Map.class);
        Assertions.assertTrue(meAfterResp.getStatusCode().is4xxClientError(), "Old access token should be rejected after role change");

        // 7) Attempt to refresh using old refresh token should fail
        Map<String, String> refreshBody = Map.of("refreshToken", userRefresh);
        ResponseEntity<Map> refreshResp = restTemplate.postForEntity("/api/auth/refresh", refreshBody, Map.class);
        Assertions.assertTrue(refreshResp.getStatusCode().is4xxClientError(), "Old refresh token should be invalidated");
    }
}
