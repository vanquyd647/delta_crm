package dentalbackend.infrastructure.persistence;

import dentalbackend.domain.Service;
import dentalbackend.domain.port.ServicePort;
import dentalbackend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServicePort {
    private final ServiceRepository repo;

    @Override
    public List<Service> findAll() {
        return repo.findAll();
    }

    @Override
    public Optional<Service> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Service save(Service s) {
        return repo.save(s);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repo.existsByName(name);
    }
}

