package dentalbackend.controller;

import dentalbackend.common.ApiResponse;
import dentalbackend.dto.UpdateProfileRequest;
import dentalbackend.dto.UserProfileResponse;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.application.userprofile.UserProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileUseCase profileService;
    private final UserUseCase userUseCase;

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails principal) {
        var user = userUseCase.findByUsernameOrEmail(principal.getUsername()).orElseThrow();
        return profileService.getByUserId(user.getId())
                .map(ApiResponse::ok)
                .orElse(ApiResponse.error("Profile not found"));
    }

    @PatchMapping
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserDetails principal,
                                                           @RequestBody UpdateProfileRequest req) {
        var user = userUseCase.findByUsernameOrEmail(principal.getUsername()).orElseThrow();
        var updated = profileService.update(user.getId(), req);
        return ApiResponse.ok(updated);
    }
}
