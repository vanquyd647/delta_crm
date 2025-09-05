package dentalbackend.application.userprofile;

import dentalbackend.dto.UpdateProfileRequest;
import dentalbackend.dto.UserProfileResponse;

import java.util.Optional;

public interface UserProfileUseCase {
    Optional<UserProfileResponse> getByUserId(Long userId);
    UserProfileResponse update(Long userId, UpdateProfileRequest req);
}

