package dentalbackend.legacy_service_backup;

import dentalbackend.application.user.UserUseCase;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.dto.UpdatePreferencesRequest;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

// Legacy backup - intentionally not a Spring bean. Use application layer implementations instead.
@RequiredArgsConstructor
public class UserService {
    private final UserUseCase userUseCase;

    public UserEntity createUser(String username, String email, String rawPassword, UserRole role) {
        return userUseCase.createUser(username, email, rawPassword, role);
    }

    public Optional<UserEntity> findByUsernameOrEmail(String usernameOrEmail) {
        return userUseCase.findByUsernameOrEmail(usernameOrEmail);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userUseCase.findByEmail(email);
    }

    public UserEntity save(UserEntity user) {
        return userUseCase.save(user);
    }

    public Optional<UserEntity> findById(Long id) {
        return userUseCase.findById(id);
    }

    public UserEntity updatePreferences(String username, UpdatePreferencesRequest req) {
        return userUseCase.updatePreferences(username, req);
    }

    public UserEntity updateDebt(Long id, Double debt) {
        return userUseCase.updateDebt(id, debt);
    }

    public UserEntity updateServiceStatus(Long id, String status) {
        return userUseCase.updateServiceStatus(id, status);
    }

    public void deleteUser(Long id) {
        userUseCase.deleteUser(id);
    }
}
