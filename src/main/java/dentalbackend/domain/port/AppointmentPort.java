package dentalbackend.domain.port;

import dentalbackend.domain.Appointment;
import dentalbackend.domain.UserEntity;

import java.util.List;
import java.util.Optional;

public interface AppointmentPort {
    Optional<Appointment> findById(Long id);
    Optional<Appointment> findByIdFetch(Long id);
    Appointment save(Appointment a);
    void deleteById(Long id);
    List<Appointment> findByDentistIdFetch(Long dentistId);
    List<Appointment> findByCustomerIdFetch(Long customerId);
    List<Appointment> findAllWithRelations();
}

