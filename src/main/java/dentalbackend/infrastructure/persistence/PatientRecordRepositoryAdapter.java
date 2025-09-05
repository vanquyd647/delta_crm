package dentalbackend.infrastructure.persistence;

import dentalbackend.domain.PatientRecord;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.port.PatientRecordPort;
import dentalbackend.repository.PatientRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PatientRecordRepositoryAdapter implements PatientRecordPort {
    private final PatientRecordRepository repo;

    @Override
    public PatientRecord save(PatientRecord r) {
        return repo.save(r);
    }

    @Override
    public Optional<PatientRecord> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<PatientRecord> findByPatient(UserEntity patient) {
        return repo.findByPatient(patient);
    }

    @Override
    public List<PatientRecord> findByDentist(UserEntity dentist) {
        return repo.findByDentist(dentist);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}

