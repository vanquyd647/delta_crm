package dentalbackend.application.auth.impl;

import dentalbackend.application.auth.AuthUseCase;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.dto.JwtResponse;
import dentalbackend.dto.LoginRequest;
import dentalbackend.dto.RefreshRequest;
import dentalbackend.dto.RegisterRequest;
import dentalbackend.captcha.CaptchaVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import dentalbackend.ratelimit.RateLimiterService;
import dentalbackend.security.jwt.JwtUtil;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Date;
import java.security.MessageDigest;
import java.util.HexFormat;

import dentalbackend.service.EmailService;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final UserUseCase userUseCase;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final RateLimiterService rateLimiter;
    private final CaptchaVerifier captchaVerifier;
    private final EmailService emailService; // EmailService remains in service package
    private final AuthenticationManager authenticationManager;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.jwt.refresh-ttl-ms:259200000}") // 3 days
    private long refreshTtlMs;

    private String accessSetKey(String username) { return "accesss:" + username; }
    private String blacklistKeyHash(String hash) { return "blacklist:access:" + hash; }
    private String accessMetaKey(String hash) { return "access_meta:" + hash; }
    private String refreshSetKey(String username) { return "refreshs:" + username; }

    private String tokenHash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            return UUID.nameUUIDFromBytes(token.getBytes()).toString().replace("-", "");
        }
    }

    @Override
    public void register(RegisterRequest req) {
        if (!captchaVerifier.verify(req.getCaptchaToken())) {
            throw new RuntimeException("Captcha verification failed");
        }
        if (userUseCase.findByUsernameOrEmail(req.getUsername()).isPresent()) throw new RuntimeException("Username exists");
        if (userUseCase.findByEmail(req.getEmail()).isPresent()) throw new RuntimeException("Email exists");

        UserEntity u = userUseCase.createUser(req.getUsername(), req.getEmail(), req.getPassword(), UserRole.CUSTOMER);

        String token = UUID.randomUUID().toString();
        redis.opsForValue().set("verify:" + token, u.getEmail(), Duration.ofHours(24));
        String link = appBaseUrl + "/api/auth/verify?token=" + token;
        emailService.send(u.getEmail(), "Xác thực email", "Nhấp để xác thực: " + link);
    }

    @Override
    public void verifyEmail(String token) {
        String email = redis.opsForValue().get("verify:" + token);
        if (!StringUtils.hasText(email)) throw new RuntimeException("Token invalid/expired");
        var userOpt = userUseCase.findByEmail(email);
        var user = userOpt.orElseThrow();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userUseCase.save(user);
        redis.delete("verify:" + token);
    }

    @Override
    public JwtResponse login(LoginRequest req) {
        boolean allowed = rateLimiter.isAllowed("login:" + req.getUsernameOrEmail(), 10, Duration.ofMinutes(1));
        if (!allowed) throw new RuntimeException("Too many requests");

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsernameOrEmail(), req.getPassword()));
        String username = auth.getName();
        var user = userUseCase.findByUsernameOrEmail(username).orElseThrow();

        String access = jwtUtil.generateToken(user.getUsername(), Map.of("role", "ROLE_" + user.getRole().name()));
        String refresh = UUID.randomUUID().toString();
        redis.opsForValue().set("refresh:" + refresh, user.getUsername(), Duration.ofMillis(refreshTtlMs));
        redis.opsForSet().add(refreshSetKey(user.getUsername()), refresh);
        String hash = tokenHash(access);
        redis.opsForSet().add(accessSetKey(user.getUsername()), hash);
        redis.expire(accessSetKey(user.getUsername()), Duration.ofMillis(refreshTtlMs));
        try {
            Date exp = jwtUtil.extractClaim(access, claims -> claims.getExpiration());
            long ttlMs = exp.getTime() - System.currentTimeMillis();
            if (ttlMs > 0) {
                redis.opsForValue().set(accessMetaKey(hash), "1", Duration.ofMillis(ttlMs));
            }
        } catch (Exception ignored) {}

        return new JwtResponse(access, refresh, "Bearer");
    }

    @Override
    public JwtResponse refresh(RefreshRequest req) {
        String username = redis.opsForValue().get("refresh:" + req.getRefreshToken());
        if (!StringUtils.hasText(username)) throw new RuntimeException("Invalid refresh token");
        var user = userUseCase.findByUsernameOrEmail(username).orElseThrow();
        String newAccess = jwtUtil.generateToken(user.getUsername(), Map.of("role", "ROLE_" + user.getRole().name()));
        String newRefresh = UUID.randomUUID().toString();
        redis.delete("refresh:" + req.getRefreshToken());
        redis.opsForValue().set("refresh:" + newRefresh, user.getUsername(), Duration.ofMillis(refreshTtlMs));
        redis.opsForSet().remove(refreshSetKey(user.getUsername()), req.getRefreshToken());
        redis.opsForSet().add(refreshSetKey(user.getUsername()), newRefresh);
        String newHash = tokenHash(newAccess);
        redis.opsForSet().add(accessSetKey(user.getUsername()), newHash);
        redis.expire(accessSetKey(user.getUsername()), Duration.ofMillis(refreshTtlMs));
        try {
            Date exp = jwtUtil.extractClaim(newAccess, claims -> claims.getExpiration());
            long ttlMs = exp.getTime() - System.currentTimeMillis();
            if (ttlMs > 0) {
                redis.opsForValue().set(accessMetaKey(newHash), "1", Duration.ofMillis(ttlMs));
            }
        } catch (Exception ignored) {}
        return new JwtResponse(newAccess, newRefresh, "Bearer");
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) return;
        String username = redis.opsForValue().get("refresh:" + refreshToken);
        if (StringUtils.hasText(username)) {
            redis.opsForSet().remove(refreshSetKey(username), refreshToken);
        }
        redis.delete("refresh:" + refreshToken);
    }

    @Override
    public void invalidateUserRefreshTokens(String username) {
        if (!StringUtils.hasText(username)) return;
        String setKey = refreshSetKey(username);
        Set<String> tokens = redis.opsForSet().members(setKey);
        if (tokens != null) {
            for (String t : tokens) {
                redis.delete("refresh:" + t);
            }
        }
        redis.delete(setKey);
    }

    @Override
    public void invalidateUserAccessTokens(String username) {
        if (!StringUtils.hasText(username)) return;
        String setKey = accessSetKey(username);
        Set<String> hashes = redis.opsForSet().members(setKey);
        if (hashes != null) {
            for (String h : hashes) {
                String metaKey = accessMetaKey(h);
                Long ttlSeconds = redis.getExpire(metaKey);
                if (ttlSeconds != null && ttlSeconds > 0) {
                    redis.opsForValue().set(blacklistKeyHash(h), "1", Duration.ofSeconds(ttlSeconds));
                } else {
                    redis.opsForValue().set(blacklistKeyHash(h), "1", Duration.ofMinutes(5));
                }
                redis.delete(metaKey);
            }
        }
        redis.delete(setKey);
    }
}
