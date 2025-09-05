package dentalbackend.repository;

import dentalbackend.domain.UserProfile;
import dentalbackend.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(UserEntity user);
}

