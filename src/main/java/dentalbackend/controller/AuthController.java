package dentalbackend.controller;

import dentalbackend.dto.LoginRequest;
import dentalbackend.dto.RefreshRequest;
import dentalbackend.dto.RegisterRequest;
import dentalbackend.application.auth.AuthUseCase;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.common.ApiResponse;
import dentalbackend.domain.UserEntity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authService;
    private final UserUseCase userUseCase;

    @Value("${app.auth.set-cookies:true}")
    private boolean setCookies;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.frontend.oauth-success-path:/auth/oauth-success}")
    private String oauthSuccessPath;

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Registered. Please check your email to verify.", null);
    }

    @GetMapping("/verify")
    public ApiResponse<?> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Email verified", null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                  @RequestParam(defaultValue = "false") boolean saveCookies,
                                  HttpServletResponse resp) {
        var loginResponse = authService.login(req);

        // Fetch user info (username/email/role) for redirect
        UserEntity user = userUseCase.findByUsernameOrEmail(req.getUsernameOrEmail()).orElse(null);

        // Always set cross-origin cookies for redirect flow when configured
        if (setCookies || saveCookies) {
            addCrossOriginCookie(resp, "access_token", loginResponse.getAccessToken(), 3600);
            addCrossOriginCookie(resp, "refresh_token", loginResponse.getRefreshToken(), 259200);
        }

        String role = user != null && user.getRole() != null ? "ROLE_" + user.getRole().name() : null;
        String email = user != null ? user.getEmail() : null;
        String username = user != null ? user.getUsername() : req.getUsernameOrEmail();

        // Always redirect to frontend OAuth success path with same params as OAuth2 flow
        try {
            String redirectUrl = frontendUrl + oauthSuccessPath +
                    "?access_token=" + URLEncoder.encode(loginResponse.getAccessToken(), StandardCharsets.UTF_8) +
                    "&refresh_token=" + URLEncoder.encode(loginResponse.getRefreshToken(), StandardCharsets.UTF_8) +
                    "&token_type=" + URLEncoder.encode(loginResponse.getTokenType(), StandardCharsets.UTF_8) +
                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                    (email != null ? "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) : "") +
                    (role != null ? "&role=" + URLEncoder.encode(role, StandardCharsets.UTF_8) : "") +
                    "&cookies_set=" + setCookies +
                    "&avatar_url=" + URLEncoder.encode((user != null && user.getAvatarUrl() != null) ? user.getAvatarUrl() : "", StandardCharsets.UTF_8);

            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String acceptHeader = request.getHeader("Accept");
            boolean isApiClient = acceptHeader != null && acceptHeader.contains("application/json");

            if (!isApiClient && saveCookies) {
                resp.sendRedirect(redirectUrl);
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            Map<String, Object> tokenMap = Map.of(
                "access_token", loginResponse.getAccessToken(),
                "refresh_token", loginResponse.getRefreshToken(),
                "token_type", loginResponse.getTokenType(),
                "username", username,
                "email", email,
                "role", role,
                "cookies_set", setCookies,
                "avatar_url", user != null && user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            );

            return ResponseEntity.ok(tokenMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Redirect failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@RequestBody(required = false) RefreshRequest req,
                                  @RequestParam(defaultValue = "auto") String source,
                                  HttpServletRequest request,
                                  HttpServletResponse resp) {
        String refreshToken = null;

        // ✅ Flexible token source selection
        switch (source.toLowerCase()) {
            case "body":
                if (req != null) refreshToken = req.getRefreshToken();
                break;
            case "cookie":
                refreshToken = getTokenFromCookie(request, "refresh_token");
                break;
            case "auto":
            default:
                // Ưu tiên body, fallback cookie
                if (req != null && req.getRefreshToken() != null) {
                    refreshToken = req.getRefreshToken();
                } else {
                    refreshToken = getTokenFromCookie(request, "refresh_token");
                }
                break;
        }

        if (refreshToken == null) {
            return ApiResponse.error("Refresh token not found in " + source);
        }

        var refreshResponse = authService.refresh(new RefreshRequest(refreshToken));

        // ✅ Cập nhật cookies nếu có token từ cookie hoặc được config
        if (setCookies || getTokenFromCookie(request, "refresh_token") != null) {
            addCookie(resp, "access_token", refreshResponse.getAccessToken(), 3600);
            addCookie(resp, "refresh_token", refreshResponse.getRefreshToken(), 259200);
        }

        return ApiResponse.ok(refreshResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestBody(required = false) RefreshRequest req,
                                 HttpServletRequest request,
                                 HttpServletResponse resp) {
        String refreshToken;

        if (req != null && req.getRefreshToken() != null) {
            refreshToken = req.getRefreshToken();
        } else {
            refreshToken = getTokenFromCookie(request, "refresh_token");
        }

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // ✅ Luôn xóa cookies khi logout
        clearCookie(resp, "access_token");
        clearCookie(resp, "refresh_token");

        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Logged out from all storage", null);
    }

    // ✅ Management endpoints cho FE
    @GetMapping("/tokens")
    public ApiResponse<Map<String, Object>> getTokens(HttpServletRequest request) {
        String accessFromCookie = getTokenFromCookie(request, "access_token");
        String refreshFromCookie = getTokenFromCookie(request, "refresh_token");

        Map<String, Object> tokenData = Map.of(
                "cookieTokens", Map.of(
                        "access", accessFromCookie != null ? "***" + accessFromCookie.substring(Math.max(0, accessFromCookie.length() - 8)) : null,
                        "refresh", refreshFromCookie != null ? refreshFromCookie : null,
                        "available", accessFromCookie != null || refreshFromCookie != null
                ),
                "instruction", Map.of(
                        "localStorage", "Save tokens from login/OAuth2 response to localStorage",
                        "cookies", "Tokens automatically saved in httpOnly cookies",
                        "usage", "Use Authorization header for API calls, cookies as fallback"
                )
        );

        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Token status", tokenData);
    }

    @PostMapping("/clear-cookies")
    public ApiResponse<?> clearCookies(HttpServletResponse resp) {
        clearCookie(resp, "access_token");
        clearCookie(resp, "refresh_token");
        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Cookies cleared", null);
    }

    @GetMapping("/oauth2/google-url")
    public ApiResponse<?> googleUrl() {
        return ApiResponse.ok("/oauth2/authorization/google");
    }

    @GetMapping("/whoami")
    public ApiResponse<Map<String, Object>> whoami(HttpServletRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ApiResponse.error("Not authenticated");
        }

        String tokenSource = "unknown";
        String authHeader = request.getHeader("Authorization");
        String cookieToken = getTokenFromCookie(request, "access_token");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenSource = "Authorization header";
        } else if (cookieToken != null) {
            tokenSource = "access_token cookie";
        }

        // ✅ Handle both UserDetails and OAuth2User
        Object principal = auth.getPrincipal();
        String username;
        Object authorities;
        String authType;

        if (principal instanceof UserDetails) {
            // 🔹 Regular login (username/password)
            UserDetails userDetails = (UserDetails) principal;
            username = userDetails.getUsername();
            authorities = userDetails.getAuthorities();
            authType = "UserDetails (Regular Login)";
        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser) {
            // 🔹 OAuth2 OIDC login (Google, etc.)
            org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser =
                    (org.springframework.security.oauth2.core.oidc.user.OidcUser) principal;
            username = oidcUser.getName(); // hoặc oidcUser.getEmail()
            authorities = oidcUser.getAuthorities();
            authType = "OidcUser (OAuth2 Login)";
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            // 🔹 OAuth2 login (GitHub, etc.)
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User =
                    (org.springframework.security.oauth2.core.user.OAuth2User) principal;
            username = oauth2User.getName();
            authorities = oauth2User.getAuthorities();
            authType = "OAuth2User (OAuth2 Login)";
        } else {
            // 🔹 Unknown principal type
            username = principal.toString();
            authorities = auth.getAuthorities();
            authType = "Unknown (" + principal.getClass().getSimpleName() + ")";
        }

        Map<String, Object> userData = Map.of(
                "username", username,
                "authorities", authorities,
                "tokenSource", tokenSource,
                "authenticated", true,
                "authType", authType,
                "principalClass", principal.getClass().getSimpleName()
        );

        return ApiResponse.ok("Current user info", userData);
    }


    @GetMapping("/test-auth-methods")
    public ApiResponse<Map<String, Object>> testAuthMethods(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String cookieToken = getTokenFromCookie(request, "access_token");

        Map<String, Object> testData = Map.of(
                "authorizationHeader", Map.of(
                        "present", authHeader != null,
                        "valid", authHeader != null && authHeader.startsWith("Bearer "),
                        "preview", authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : null
                ),
                "accessTokenCookie", Map.of(
                        "present", cookieToken != null,
                        "preview", cookieToken != null ? cookieToken.substring(0, Math.min(20, cookieToken.length())) + "..." : null
                ),
                "recommendation", "Authorization header has priority over cookies"
        );

        // ✅ Sửa: message trước, data sau
        return ApiResponse.ok("Authentication methods test", testData);
    }

    // Helper methods
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void addCookie(HttpServletResponse resp, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        resp.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse resp, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }

    // New helper for cross-origin cookies
    private void addCrossOriginCookie(HttpServletResponse resp, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "None");
        resp.addCookie(cookie);
    }
}
