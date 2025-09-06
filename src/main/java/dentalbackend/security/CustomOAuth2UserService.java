package dentalbackend.security;

import dentalbackend.domain.AuthProvider;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.domain.UserPreferences;
import dentalbackend.domain.UserProfile;
import dentalbackend.repository.UserRepository;
import dentalbackend.repository.UserProfileRepository;
import dentalbackend.repository.UserPreferencesRepository;
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
    private final UserProfileRepository profileRepo;
    private final UserPreferencesRepository preferencesRepo;
    private final UserUseCase userUseCase;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attrs = oAuth2User.getAttributes();

            log.debug("OAuth2 user attributes: {}", attrs);

            // Standard Google OIDC attributes
            String sub = (String) attrs.get("sub");
            String email = (String) attrs.get("email");
            String name = (String) attrs.getOrDefault("name", email);
            Boolean emailVerified = (Boolean) attrs.getOrDefault("email_verified", Boolean.FALSE);
            String picture = attrs.get("picture") != null ? String.valueOf(attrs.get("picture")) : null;

            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Google did not return email (require openid email scope)");
            }

            log.info("Processing OAuth2 user with email: {}", email);

            // Find or create user by email
            UserEntity user = userRepo.findByEmail(email).orElse(null);
            if (user == null) {
                log.info("Creating new user for OAuth2 login: {}", email);

                String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
                String username = ensureUniqueUsername(base);
                String randomPassword = "google_" + UUID.randomUUID();

                user = userUseCase.createUser(username, email, randomPassword, UserRole.CUSTOMER);
            } else {
                log.info("Found existing user for OAuth2 login: {}", email);
            }

            // Link OAuth info and update avatar if present
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(sub);
            if (name != null && !name.isBlank()) user.setFullName(name);
            if (picture != null && !picture.isBlank()) user.setAvatarUrl(picture);
            user.setEmailVerified(Boolean.TRUE.equals(emailVerified) || user.isEmailVerified());
            user.setEnabled(true);

            // Save user and use a final reference for subsequent lambda usage
            UserEntity savedUser = userRepo.save(user);

            // Ensure and sync profile using savedUser (final)
            try {
                profileRepo.findByUser(savedUser).ifPresentOrElse(p -> {
                    if ((p.getAvatarUrl() == null || p.getAvatarUrl().isBlank()) && savedUser.getAvatarUrl() != null && !savedUser.getAvatarUrl().isBlank()) {
                        p.setAvatarUrl(savedUser.getAvatarUrl());
                        profileRepo.save(p);
                    }
                }, () -> {
                    UserProfile p = UserProfile.builder()
                            .user(savedUser)
                            .phone(null)
                            .birthDate(null)
                            .gender(null)
                            .address(null)
                            .avatarUrl(savedUser.getAvatarUrl())
                            .emergencyContact(null)
                            .build();
                    profileRepo.save(p);
                });
            } catch (Exception ex) {
                log.warn("Failed to sync user profile for {}: {}", savedUser.getUsername(), ex.getMessage());
            }

            // Ensure preferences
            try {
                preferencesRepo.findByUser(savedUser).ifPresentOrElse(pref -> {
                    // nothing
                }, () -> {
                    UserPreferences pref = UserPreferences.builder()
                            .user(savedUser)
                            .themePreference(savedUser.getThemePreference() != null ? savedUser.getThemePreference() : "light")
                            .languagePreference(savedUser.getLanguagePreference() != null ? savedUser.getLanguagePreference() : "vi")
                            .notificationPreference(savedUser.getNotificationPreference() != null ? savedUser.getNotificationPreference() : "EMAIL")
                            .timezone(null)
                            .build();
                    preferencesRepo.save(pref);
                });
            } catch (Exception ex) {
                log.warn("Failed to sync user preferences for {}: {}", savedUser.getUsername(), ex.getMessage());
            }

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
