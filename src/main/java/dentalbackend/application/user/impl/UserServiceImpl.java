package dentalbackend.application.user.impl;

import dentalbackend.application.user.UserUseCase;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserPreferences;
import dentalbackend.domain.UserProfile;
import dentalbackend.domain.UserRole;
import dentalbackend.domain.Role;
import dentalbackend.dto.UpdatePreferencesRequest;
import dentalbackend.repository.UserPreferencesRepository;
import dentalbackend.repository.UserProfileRepository;
import dentalbackend.repository.UserRepository;
import dentalbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("applicationUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserUseCase {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository profileRepo;
    private final UserPreferencesRepository preferencesRepo;
    private final RoleRepository roleRepository;

    @Override
    public UserEntity createUser(String username, String email, String rawPassword, UserRole role) {
        if (userRepo.existsByUsername(username)) throw new IllegalArgumentException("Username exists");
        if (userRepo.existsByEmail(email)) throw new IllegalArgumentException("Email exists");

        String hash = passwordEncoder.encode(rawPassword == null ? "" : rawPassword);
        UserEntity u = UserEntity.builder()
                .username(username)
                .email(email)
                .passwordHash(hash)
                .role(role) // transient; will resolve Role entity below
                .provider(dentalbackend.domain.AuthProvider.LOCAL)
                .providerId(null)
                .fullName(null)
                .emailVerified(role == UserRole.ADMIN) // enable emailVerified for admin bootstrap
                .enabled(role == UserRole.ADMIN) // enable admin by default
                .themePreference(null)
                .languagePreference(null)
                .notificationPreference(null)
                .debt(0.0)
                .serviceStatus(null)
                .build();

        // Resolve Role entity and attach
        try {
            Role roleEntity = roleRepository.findByName(role).orElse(null);
            u.setRoleEntity(roleEntity);
        } catch (Exception ex) {
            // best-effort: leave null, permission checks still rely on transient enum until persisted
        }

        // Save user first to obtain id
        UserEntity saved = userRepo.save(u);

        // Create empty profile if not exists
        UserProfile profile = UserProfile.builder()
                .user(saved)
                .phone(null)
                .birthDate(null)
                .gender(null)
                .address(null)
                .avatarUrl(saved.getAvatarUrl())
                .emergencyContact(null)
                .build();
        profileRepo.save(profile);

        // Create preferences with sensible defaults
        UserPreferences prefs = UserPreferences.builder()
                .user(saved)
                .themePreference(saved.getThemePreference() != null ? saved.getThemePreference() : "light")
                .languagePreference(saved.getLanguagePreference() != null ? saved.getLanguagePreference() : "vi")
                .notificationPreference(saved.getNotificationPreference() != null ? saved.getNotificationPreference() : "EMAIL")
                .timezone(null)
                .build();
        preferencesRepo.save(prefs);

        return saved;
    }

    @Override
    public Optional<UserEntity> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepo.findByUsernameOrEmail(usernameOrEmail);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    @Override
    public UserEntity updatePreferences(String username, UpdatePreferencesRequest request) {
        var user = userRepo.findByUsername(username).orElseThrow();
        if (request.getThemePreference() != null) user.setThemePreference(request.getThemePreference());
        if (request.getLanguagePreference() != null) user.setLanguagePreference(request.getLanguagePreference());
        if (request.getNotificationPreference() != null) user.setNotificationPreference(request.getNotificationPreference());
        return userRepo.save(user);
    }

    @Override
    public UserEntity save(UserEntity user) {
        // New: Resolve transient role enum to Role entity to avoid duplicate role rows and ensure roleEntity is set
        try {
            if (user.getRole() != null && user.getRoleEntity() == null) {
                roleRepository.findByName(user.getRole()).ifPresent(user::setRoleEntity);
            }
        } catch (Exception ex) {
            // best-effort: ignore resolution failure
        }

        UserEntity saved = userRepo.save(user);

        // Sync or create profile
        try {
            profileRepo.findByUser(saved).ifPresentOrElse(p -> {
                if ((p.getAvatarUrl() == null || p.getAvatarUrl().isBlank()) && saved.getAvatarUrl() != null && !saved.getAvatarUrl().isBlank()) {
                    p.setAvatarUrl(saved.getAvatarUrl());
                    profileRepo.save(p);
                }
            }, () -> {
                UserProfile profile = UserProfile.builder()
                        .user(saved)
                        .phone(null)
                        .birthDate(null)
                        .gender(null)
                        .address(null)
                        .avatarUrl(saved.getAvatarUrl())
                        .emergencyContact(null)
                        .build();
                profileRepo.save(profile);
            });
        } catch (Exception ex) {
            // best-effort sync; log if logger available
        }

        // Sync or create preferences
        try {
            preferencesRepo.findByUser(saved).ifPresentOrElse(pref -> {
                // nothing for now
            }, () -> {
                UserPreferences pref = UserPreferences.builder()
                        .user(saved)
                        .themePreference(saved.getThemePreference() != null ? saved.getThemePreference() : "light")
                        .languagePreference(saved.getLanguagePreference() != null ? saved.getLanguagePreference() : "vi")
                        .notificationPreference(saved.getNotificationPreference() != null ? saved.getNotificationPreference() : "EMAIL")
                        .timezone(null)
                        .build();
                preferencesRepo.save(pref);
            });
        } catch (Exception ex) {
            // best-effort
        }

        return saved;
    }

    @Override
    public Optional<UserEntity> findById(Long userId) {
        return userRepo.findById(userId);
    }

    @Override
    public UserEntity updateDebt(Long userId, Double debt) {
        var user = userRepo.findById(userId).orElseThrow();
        user.setDebt(debt);
        return userRepo.save(user);
    }

    @Override
    public UserEntity updateServiceStatus(Long userId, String serviceStatus) {
        var user = userRepo.findById(userId).orElseThrow();
        user.setServiceStatus(serviceStatus);
        return userRepo.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepo.deleteById(userId);
    }
}
