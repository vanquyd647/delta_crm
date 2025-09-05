package dentalbackend.application.user;

import dentalbackend.domain.UserEntity;
import dentalbackend.dto.UpdatePreferencesRequest;
import dentalbackend.dto.UserProfileResponse;
import dentalbackend.dto.UpdateProfileRequest;

import java.util.Optional;

public interface UserUseCase {
    UserEntity createUser(String username, String email, String rawPassword, dentalbackend.domain.UserRole role);
    Optional<UserEntity> findByUsernameOrEmail(String usernameOrEmail);
    Optional<UserEntity> findByEmail(String email);
    UserEntity updatePreferences(String username, UpdatePreferencesRequest request);
    UserEntity save(UserEntity user);
    Optional<UserEntity> findById(Long userId);
    UserEntity updateDebt(Long userId, Double debt);
    UserEntity updateServiceStatus(Long userId, String serviceStatus);
    void deleteUser(Long userId);
}
