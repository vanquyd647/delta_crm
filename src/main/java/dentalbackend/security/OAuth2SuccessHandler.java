package dentalbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import dentalbackend.security.jwt.JwtUtil;
import dentalbackend.domain.AuthProvider;
import dentalbackend.domain.UserEntity;
import dentalbackend.repository.UserRepository;
import dentalbackend.domain.UserRole;
import dentalbackend.application.user.UserUseCase;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final UserUseCase userUseCase;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.jwt.refresh-ttl-ms:259200000}") // 3 ngày
    private long refreshTtlMs;

    @Value("${app.jwt.access-ttl-ms:3600000}") // 1 giờ
    private long accessTtlMs;

    @Value("${app.auth.set-cookies:true}")
    private boolean setCookies;

    // ✅ FE URL config
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.frontend.oauth-success-path:/auth/oauth-success}")
    private String oauthSuccessPath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication auth) throws IOException {
        try {
            OAuth2User principal = (OAuth2User) auth.getPrincipal();
            String email = (String) principal.getAttributes().get("email");

            log.debug("OAuth2 success for email: {}", email);

            if (email == null || email.isBlank()) {
                log.error("Email not found in OAuth2 response");
                redirectToFrontendWithError(resp, "Email not found in OAuth2 response");
                return;
            }

            // User creation/update logic
            UserEntity user = userRepo.findByEmail(email).orElseGet(() -> {
                log.info("Creating new user for email: {}", email);
                String name = (String) principal.getAttributes().getOrDefault("name", email);
                String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
                String username = ensureUniqueUsername(base);

                String randomPassword = "google_" + UUID.randomUUID();

                UserEntity newUser = userUseCase.createUser(username, email, randomPassword, UserRole.CUSTOMER);
                newUser.setProvider(AuthProvider.GOOGLE);
                newUser.setProviderId((String) principal.getAttributes().get("sub"));
                newUser.setFullName(name);
                newUser.setEmailVerified(true);
                newUser.setEnabled(true);

                return userRepo.save(newUser);
            });

            if (user.getProvider() == null || user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.GOOGLE);
                user.setProviderId((String) principal.getAttributes().get("sub"));
                String name = (String) principal.getAttributes().get("name");
                if (name != null && !name.isBlank()) {
                    user.setFullName(name);
                }
                user.setEmailVerified(true);
                user.setEnabled(true);
                userRepo.save(user);
            }

            // Generate tokens
            String role = "ROLE_" + user.getRole().name();
            String access = jwtUtil.generateToken(user.getUsername(), Map.of("role", role));
            String refresh = UUID.randomUUID().toString();
            redis.opsForValue().set("refresh:" + refresh, user.getUsername(), Duration.ofMillis(refreshTtlMs));

            log.info("OAuth2 login successful for user: {}", user.getUsername());

            // ✅ Set cookies với SameSite=None cho cross-origin
            if (setCookies) {
                addCrossOriginCookie(resp, "access_token", access, (int) (accessTtlMs / 1000));
                addCrossOriginCookie(resp, "refresh_token", refresh, (int) (refreshTtlMs / 1000));
            }

            // ✅ Redirect về FE với tokens trong URL params
            redirectToFrontendWithTokens(resp, access, refresh, user);
            clearAuthenticationAttributes(req);

        } catch (Exception e) {
            log.error("Error in OAuth2 success handler", e);
            redirectToFrontendWithError(resp, "Internal server error: " + e.getMessage());
        }
    }

    // ✅ Redirect về FE với tokens
    private void redirectToFrontendWithTokens(HttpServletResponse resp, String accessToken, String refreshToken, UserEntity user) throws IOException {
        try {
            String redirectUrl = frontendUrl + oauthSuccessPath +
                    "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                    "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                    "&token_type=Bearer" +
                    "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8) +
                    "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8) +
                    "&role=" + URLEncoder.encode(user.getRole().name(), StandardCharsets.UTF_8) +
                    "&cookies_set=" + setCookies;

            log.info("Redirecting OAuth2 user to: {}", redirectUrl);
            resp.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Error creating redirect URL", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect failed");
        }
    }

    // ✅ Redirect về FE với error
    private void redirectToFrontendWithError(HttpServletResponse resp, String error) throws IOException {
        String redirectUrl = frontendUrl + oauthSuccessPath +
                "?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8);

        log.warn("Redirecting OAuth2 error to: {}", redirectUrl);
        resp.sendRedirect(redirectUrl);
    }

    // ✅ Cross-origin cookies với SameSite=None; Secure
    private void addCrossOriginCookie(HttpServletResponse resp, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath("/");
        cookie.setSecure(true); // Required for SameSite=None
        cookie.setAttribute("SameSite", "None"); // Allow cross-origin
        resp.addCookie(cookie);
        log.debug("Added cross-origin httpOnly cookie: {} with maxAge: {} seconds", name, maxAgeSeconds);
    }

    private String ensureUniqueUsername(String base) {
        String candidate = (base == null || base.isBlank()) ? "user" : base;
        if (!userRepo.existsByUsername(candidate)) return candidate;
        int i = 1;
        while (userRepo.existsByUsername(candidate + i)) {
            i++;
        }
        return candidate + i;
    }
}
