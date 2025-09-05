package dentalbackend.security;

import dentalbackend.domain.AuthProvider;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.repository.UserRepository;
import dentalbackend.application.user.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepo;
    private final UserUseCase userUseCase;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attrs = oAuth2User.getAttributes();

            log.debug("OAuth2 user attributes: {}", attrs);

            // Thuộc tính chuẩn của Google OIDC
            String sub = (String) attrs.get("sub");
            String email = (String) attrs.get("email");
            String name = (String) attrs.getOrDefault("name", email);
            Boolean emailVerified = (Boolean) attrs.getOrDefault("email_verified", Boolean.FALSE);

            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Google không trả về email (cần scope: openid, email)");
            }

            log.info("Processing OAuth2 user with email: {}", email);

            // Tìm theo email (link tài khoản theo email)
            UserEntity user = userRepo.findByEmail(email).orElse(null);
            if (user == null) {
                log.info("Creating new user for OAuth2 login: {}", email);

                // Tạo username từ local-part của email, đảm bảo duy nhất
                String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
                String username = ensureUniqueUsername(base);

                // Tạo mật khẩu ngẫu nhiên (vì createUser yêu cầu rawPassword)
                String randomPassword = "google_" + UUID.randomUUID();

                user = userUseCase.createUser(username, email, randomPassword, UserRole.CUSTOMER);
            } else {
                log.info("Found existing user for OAuth2 login: {}", email);
            }

            // Link thông tin OAuth vào user
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(sub);
            if (name != null && !name.isBlank()) user.setFullName(name);
            user.setEmailVerified(Boolean.TRUE.equals(emailVerified) || user.isEmailVerified());
            user.setEnabled(true);
            userRepo.save(user);

            log.info("OAuth2 user processing completed for: {}", user.getUsername());

            return oAuth2User;

        } catch (Exception e) {
            log.error("Error in OAuth2 user service", e);
            throw new RuntimeException("Failed to process OAuth2 user", e);
        }
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
