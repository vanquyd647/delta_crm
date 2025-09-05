package dentalbackend.domain.port;

import dentalbackend.domain.Dentist;

import java.util.List;
import java.util.Optional;

public interface DentistPort {
    Optional<Dentist> findById(Long id);
    Optional<Dentist> findByUserId(Long userId);
    List<Dentist> findByActiveTrue();
    List<Dentist> findAll();
    Dentist save(Dentist d);
    void deleteById(Long id);
}

