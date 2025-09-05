package dentalbackend.repository;

import dentalbackend.domain.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DentistRepository extends JpaRepository<Dentist, Long> {
    Optional<Dentist> findByUserId(Long userId);
    List<Dentist> findByActiveTrue();
}
