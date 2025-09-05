package dentalbackend.domain.port;

import dentalbackend.domain.Payment;
import dentalbackend.domain.Appointment;

import java.util.List;
import java.util.Optional;

public interface PaymentPort {
    Payment save(Payment p);
    List<Payment> findByAppointment(Appointment appointment);
    Optional<Payment> findById(Long id);
    List<Payment> findAll();
}

