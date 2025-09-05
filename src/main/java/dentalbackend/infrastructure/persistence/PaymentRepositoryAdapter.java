package dentalbackend.infrastructure.persistence;

import dentalbackend.domain.Payment;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.port.PaymentPort;
import dentalbackend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentPort {
    private final PaymentRepository repo;

    @Override
    public Payment save(Payment p) {
        return repo.save(p);
    }

    @Override
    public List<Payment> findByAppointment(Appointment appointment) {
        return repo.findByAppointment(appointment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<Payment> findAll() {
        return repo.findAll();
    }
}

