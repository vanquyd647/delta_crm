package dentalbackend.domain.port;

import dentalbackend.domain.PatientRecord;
import dentalbackend.domain.UserEntity;

import java.util.List;
import java.util.Optional;

public interface PatientRecordPort {
    PatientRecord save(PatientRecord r);
    Optional<PatientRecord> findById(Long id);
    List<PatientRecord> findByPatient(UserEntity patient);
    List<PatientRecord> findByDentist(UserEntity dentist);
    void deleteById(Long id);
}

