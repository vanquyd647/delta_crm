package dentalbackend.repository;

import dentalbackend.domain.PatientRecord;
import dentalbackend.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRecordRepository extends JpaRepository<PatientRecord, Long> {
    List<PatientRecord> findByPatient(UserEntity patient);
    List<PatientRecord> findByDentist(UserEntity dentist);
}

