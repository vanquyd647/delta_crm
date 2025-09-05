package dentalbackend.application.auth;

import dentalbackend.dto.JwtResponse;
import dentalbackend.dto.LoginRequest;
import dentalbackend.dto.RefreshRequest;
import dentalbackend.dto.RegisterRequest;

public interface AuthUseCase {
    void register(RegisterRequest req);
    void verifyEmail(String token);
    JwtResponse login(LoginRequest req);
    JwtResponse refresh(RefreshRequest req);
    void logout(String refreshToken);
    void invalidateUserRefreshTokens(String username);
    void invalidateUserAccessTokens(String username);
}
