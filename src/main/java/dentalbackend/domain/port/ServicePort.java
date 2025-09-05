package dentalbackend.domain.port;

import dentalbackend.domain.Service;

import java.util.List;
import java.util.Optional;

public interface ServicePort {
    List<Service> findAll();
    Optional<Service> findById(Long id);
    Service save(Service s);
    void deleteById(Long id);
    boolean existsByName(String name);
}

