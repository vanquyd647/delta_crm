package dentalbackend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import dentalbackend.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepo;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ Extract token từ cả header và cookies
        String jwt = extractToken(request);
        String tokenSource = null;

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check blacklist in Redis
        try {
            String black = redis.opsForValue().get("blacklist:access:" + jwt);
            if (black != null) {
                log.debug("🚫 Access token is blacklisted, rejecting: {}", jwt);
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception ex) {
            log.debug("Redis check failed: {}", ex.getMessage());
            // continue without blacklist protection if Redis is unavailable
        }

        // Determine token source for logging
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenSource = "header";
        } else {
            tokenSource = "cookie";
        }

        String username;
        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            log.debug("Invalid JWT token from {}: {}", tokenSource, e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("✅ Authenticated user: {} via {}", username, tokenSource);
                } else {
                    log.debug("❌ Invalid token for user: {} from {}", username, tokenSource);
                }
            } catch (Exception e) {
                log.debug("❌ Authentication failed for user: {} from {}: {}", username, tokenSource, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * ✅ Extract token từ cả Authorization header và cookies
     * Ưu tiên: Authorization header > access_token cookie
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Ưu tiên Authorization header (cho API calls từ FE)
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String headerToken = authHeader.substring(7);
            log.debug("🔑 Token extracted from Authorization header");
            return headerToken;
        }

        // 2. Fallback sang access_token cookie (cho auto-auth)
        String cookieToken = getTokenFromCookie(request, "access_token");
        if (cookieToken != null) {
            log.debug("🍪 Token extracted from access_token cookie");
            return cookieToken;
        }

        log.debug("🚫 No token found in header or cookies");
        return null;
    }

    /**
     * ✅ Helper method để lấy token từ cookies
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    /**
     * ✅ Skip authentication cho các endpoints công khai
     * 🔥 QUAN TRỌNG: THÊM TẤT CẢ SWAGGER ENDPOINTS VÀO ĐÂY
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        log.debug("🔍 Checking shouldNotFilter for path: {}", path);

        // ✅ SWAGGER/OPENAPI ENDPOINTS - QUAN TRỌNG!
        if (path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/api-docs")) {
            log.debug("✅ Skipping JWT filter for Swagger endpoint: {}", path);
            return true;
        }

        // ✅ AUTH ENDPOINTS
        if (path.startsWith("/api/auth/")) {
            log.debug("✅ Skipping JWT filter for auth endpoint: {}", path);
            return true;
        }

        // ✅ OAUTH2 ENDPOINTS
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            log.debug("✅ Skipping JWT filter for OAuth2 endpoint: {}", path);
            return true;
        }

        // ✅ OTHER PUBLIC ENDPOINTS
        if (path.startsWith("/api/public/") ||
                path.equals("/") ||
                path.startsWith("/static/") ||
                path.startsWith("/favicon.ico") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/error")) {
            log.debug("✅ Skipping JWT filter for public endpoint: {}", path);
            return true;
        }

        log.debug("🔒 JWT filter will process: {}", path);
        return false;
    }
}
