package dentalbackend.infrastructure.persistence;

import dentalbackend.domain.Dentist;
import dentalbackend.domain.port.DentistPort;
import dentalbackend.repository.DentistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DentistRepositoryAdapter implements DentistPort {
    private final DentistRepository repo;

    @Override
    public Optional<Dentist> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Dentist> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public List<Dentist> findByActiveTrue() {
        return repo.findByActiveTrue();
    }

    @Override
    public List<Dentist> findAll() {
        return repo.findAll();
    }

    @Override
    public Dentist save(Dentist d) {
        return repo.save(d);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}

