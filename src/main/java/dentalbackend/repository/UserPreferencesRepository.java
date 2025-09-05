package dentalbackend.repository;

import dentalbackend.domain.UserPreferences;
import dentalbackend.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    Optional<UserPreferences> findByUser(UserEntity user);
}

