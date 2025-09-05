package dentalbackend.application.patient;

import dentalbackend.dto.PatientRecordRequest;
import dentalbackend.dto.PatientRecordResponse;

import java.util.List;

public interface PatientRecordUseCase {
    List<PatientRecordResponse> forPatient(Long patientId);
    List<PatientRecordResponse> forDentist(Long dentistId);
    PatientRecordResponse create(Long patientId, PatientRecordRequest req);
    PatientRecordResponse update(Long id, PatientRecordRequest req);
    void delete(Long id);
}

