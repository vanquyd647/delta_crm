package dentalbackend.infrastructure.persistence;

import dentalbackend.domain.Appointment;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryAdapter implements AppointmentPort {
    private final AppointmentRepository repo;

    @Override
    public Optional<Appointment> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Appointment> findByIdFetch(Long id) {
        return repo.findByIdFetch(id);
    }

    @Override
    public Appointment save(Appointment a) {
        return repo.save(a);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Override
    public List<Appointment> findByDentistIdFetch(Long dentistId) {
        return repo.findByDentistIdFetch(dentistId);
    }

    @Override
    public List<Appointment> findByCustomerIdFetch(Long customerId) {
        return repo.findByCustomerIdFetch(customerId);
    }

    @Override
    public List<Appointment> findAllWithRelations() {
        return repo.findAllWithRelations();
    }
}

