package dentalbackend.controller;

import dentalbackend.common.ApiResponse;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.application.auth.AuthUseCase;
import dentalbackend.dto.UpdatePreferencesRequest;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userService;
    private final AuthUseCase authService;

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ApiResponse.error("Unauthenticated: please provide a valid Authorization Bearer token");
        }
        return ApiResponse.ok(userService.findByUsernameOrEmail(principal.getUsername()));
    }

    @PatchMapping("/me/preferences")
    public ApiResponse<?> updatePreferences(@AuthenticationPrincipal UserDetails principal,
                                            @RequestBody UpdatePreferencesRequest req) {
        if (principal == null) {
            return ApiResponse.error("Unauthenticated: please provide a valid Authorization Bearer token");
        }
        var user = userService.findByUsernameOrEmail(principal.getUsername()).orElseThrow();
        if (req.getThemePreference() != null) user.setThemePreference(req.getThemePreference());
        if (req.getLanguagePreference() != null) user.setLanguagePreference(req.getLanguagePreference());
        if (req.getNotificationPreference() != null) user.setNotificationPreference(req.getNotificationPreference());
        userService.save(user);
        return ApiResponse.ok("Preferences updated", user);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ApiResponse<?> createUser(@RequestBody UserEntity user) {
        var created = userService.save(user);
        return ApiResponse.ok("User created", created);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody UserEntity updateReq) {
        var user = userService.findById(id).orElseThrow();
        user.setUsername(updateReq.getUsername());
        user.setEmail(updateReq.getEmail());
        user.setRole(updateReq.getRole());
        user.setFullName(updateReq.getFullName());
        user.setEnabled(updateReq.isEnabled());
        user.setDebt(updateReq.getDebt());
        user.setServiceStatus(updateReq.getServiceStatus());
        var updated = userService.save(user);
        // Invalidate tokens via authUseCase
        authService.invalidateUserRefreshTokens(updated.getUsername());
        authService.invalidateUserAccessTokens(updated.getUsername());
        return ApiResponse.ok("User updated", updated);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/role")
    public ApiResponse<?> changeRole(@PathVariable Long id, @RequestParam("role") UserRole role) {
        var user = userService.findById(id).orElseThrow();
        user.setRole(role);
        var updated = userService.save(user);
        authService.invalidateUserRefreshTokens(updated.getUsername());
        authService.invalidateUserAccessTokens(updated.getUsername());
        return ApiResponse.ok("Role updated", updated);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok("User deleted", null);
    }

    @PatchMapping("/{id}/debt")
    public ApiResponse<?> updateDebt(@PathVariable Long id, @RequestParam Double debt) {
        var updated = userService.updateDebt(id, debt);
        return ApiResponse.ok("Debt updated", updated);
    }

    @PatchMapping("/{id}/service-status")
    public ApiResponse<?> updateServiceStatus(@PathVariable Long id, @RequestParam String serviceStatus) {
        var updated = userService.updateServiceStatus(id, serviceStatus);
        return ApiResponse.ok("Service status updated", updated);
    }
}
