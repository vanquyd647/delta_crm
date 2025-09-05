package dentalbackend;

import dentalbackend.common.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.flyway.enabled=false",
        "app.bootstrap.create-admin=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoleChangeNoDockerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // Mock Redis template and provide in-memory semantics
    @MockBean
    private StringRedisTemplate redis;

    @MockBean
    private dentalbackend.service.EmailService emailService;

    @MockBean
    private dentalbackend.ratelimit.RateLimiterService rateLimiter;

    @BeforeEach
    public void setupRedisMocks() {
        // In-memory maps to simulate Redis
        Map<String, String> kv = new ConcurrentHashMap<>();
        Map<String, Set<String>> sets = new ConcurrentHashMap<>();
        Map<String, Long> expirations = new ConcurrentHashMap<>();

        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        SetOperations<String, String> setOps = org.mockito.Mockito.mock(SetOperations.class);

        // opsForValue().set(key, value, timeout)
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);

        try {
            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                Object third = invocation.getArgument(2);
                if (third instanceof Duration) {
                    Duration d = (Duration) third;
                    kv.put(key, value);
                    expirations.put(key, System.currentTimeMillis() + d.toMillis());
                } else {
                    kv.put(key, value);
                }
                return null;
            }).when(valueOps).set(any(String.class), any(String.class), any(Duration.class));

            when(valueOps.get(any(String.class))).thenAnswer(invocation -> {
                String k = invocation.getArgument(0);
                Long exp = expirations.get(k);
                if (exp != null && System.currentTimeMillis() > exp) {
                    kv.remove(k);
                    expirations.remove(k);
                    return null;
                }
                return kv.get(k);
            });

            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                kv.remove(key);
                expirations.remove(key);
                return null;
            }).when(valueOps).getOperations(); // not used directly but safe

            // Set operations
            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                sets.computeIfAbsent(key, k -> Collections.synchronizedSet(new HashSet<>())).add(value);
                return 1L;
            }).when(setOps).add(any(String.class), any(String.class));

            when(setOps.members(any(String.class))).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return sets.getOrDefault(key, Collections.emptySet());
            });

            org.mockito.Mockito.doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                Set<String> s = sets.get(key);
                if (s != null) s.remove(value);
                return 1L;
            }).when(setOps).remove(any(String.class), any(String.class));

            when(redis.getExpire(any(String.class))).thenAnswer(invocation -> {
                String k = invocation.getArgument(0);
                Long exp = expirations.get(k);
                if (exp == null) return -2L; // key does not exist
                long rem = exp - System.currentTimeMillis();
                return rem > 0 ? (rem / 1000L) : -2L;
            });

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // Rate limiter allow
        when(rateLimiter.isAllowed(any(String.class), any(Integer.class), any(Duration.class))).thenReturn(true);
    }

    @Test
    public void roleChangeShouldInvalidateTokens_noDocker() throws Exception {
        // Register test user
        Map<String, Object> registerReq = Map.of(
                "username", "int-test-user",
                "email", "int-user@example.com",
                "password", "TestPass123!",
                "captchaToken", "dummy"
        );
        ResponseEntity<ApiResponse> regResp = restTemplate.postForEntity("/api/auth/register", registerReq, ApiResponse.class);
        Assertions.assertTrue(regResp.getStatusCode().is2xxSuccessful(), "Register should succeed");

        // Login as admin (bootstrap should create admin)
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

        // Login as test user
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

        // Use user's access token to call /api/users/me (should succeed)
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccess);
        HttpEntity<Void> userReq = new HttpEntity<>(userHeaders);
        ResponseEntity<Map> meResp = restTemplate.exchange("/api/users/me", HttpMethod.GET, userReq, Map.class);
        Assertions.assertEquals(HttpStatus.OK, meResp.getStatusCode());

        // As admin, change test user's role to DENTIST
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

        // Old access token should now be rejected when calling protected endpoint
        ResponseEntity<Map> meAfterResp = restTemplate.exchange("/api/users/me", HttpMethod.GET, userReq, Map.class);
        Assertions.assertTrue(meAfterResp.getStatusCode().is4xxClientError(), "Old access token should be rejected after role change");

        // Attempt to refresh using old refresh token should fail
        Map<String, String> refreshBody = Map.of("refreshToken", userRefresh);
        ResponseEntity<Map> refreshResp = restTemplate.postForEntity("/api/auth/refresh", refreshBody, Map.class);
        Assertions.assertTrue(refreshResp.getStatusCode().is4xxClientError(), "Old refresh token should be invalidated");
    }
}

