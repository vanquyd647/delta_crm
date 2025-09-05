package dentalbackend.application.user.impl;

import dentalbackend.application.user.UserUseCase;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.dto.UpdatePreferencesRequest;
import dentalbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("applicationUserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserUseCase {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserEntity createUser(String username, String email, String rawPassword, UserRole role) {
        if (userRepo.existsByUsername(username)) throw new IllegalArgumentException("Username exists");
        if (userRepo.existsByEmail(email)) throw new IllegalArgumentException("Email exists");

        String hash = passwordEncoder.encode(rawPassword == null ? "" : rawPassword);
        UserEntity u = UserEntity.builder()
                .username(username)
                .email(email)
                .passwordHash(hash)
                .role(role)
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

        return userRepo.save(u);
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
        return userRepo.save(user);
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

