package dentalbackend.repository;

import dentalbackend.domain.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    StaffProfile findByUserId(Long userId);
}

