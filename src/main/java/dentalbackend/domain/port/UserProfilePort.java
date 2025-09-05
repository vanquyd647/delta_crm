package dentalbackend.domain.port;

import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserProfile;

import java.util.Optional;

public interface UserProfilePort {
    Optional<UserProfile> findByUser(UserEntity user);
    UserProfile save(UserProfile profile);
}

