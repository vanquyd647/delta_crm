package dentalbackend.repository;

import dentalbackend.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import dentalbackend.domain.UserEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDentistAndScheduledTimeBetween(UserEntity dentist, Instant start, Instant end);
    List<Appointment> findByDentist(UserEntity dentist);
    List<Appointment> findByCustomer(UserEntity customer);

    // FK-based queries (kept for compatibility)
    List<Appointment> findByDentistId(Long dentistId);
    List<Appointment> findByCustomerId(Long customerId);

    // FETCH variants to eagerly load relations so DTO mapping works outside of session
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.dentist LEFT JOIN FETCH a.receptionist LEFT JOIN FETCH a.service WHERE a.dentist.id = :dentistId")
    List<Appointment> findByDentistIdFetch(@Param("dentistId") Long dentistId);

    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.dentist LEFT JOIN FETCH a.receptionist LEFT JOIN FETCH a.service WHERE a.customer.id = :customerId")
    List<Appointment> findByCustomerIdFetch(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.dentist LEFT JOIN FETCH a.receptionist LEFT JOIN FETCH a.service")
    List<Appointment> findAllWithRelations();

    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.dentist LEFT JOIN FETCH a.receptionist LEFT JOIN FETCH a.service WHERE a.id = :id")
    Optional<Appointment> findByIdFetch(@Param("id") Long id);
}
