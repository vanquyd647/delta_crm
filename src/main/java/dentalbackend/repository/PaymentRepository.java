package dentalbackend.repository;

import dentalbackend.domain.Payment;
import dentalbackend.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByAppointment(Appointment appointment);
}

