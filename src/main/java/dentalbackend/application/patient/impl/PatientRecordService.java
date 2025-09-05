package dentalbackend.application.patient.impl;

import dentalbackend.application.patient.PatientRecordUseCase;
import dentalbackend.domain.PatientRecord;
import dentalbackend.domain.UserEntity;
import dentalbackend.dto.PatientRecordRequest;
import dentalbackend.dto.PatientRecordResponse;
import dentalbackend.domain.port.PatientRecordPort;
import dentalbackend.domain.port.UserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientRecordService implements PatientRecordUseCase {
    private final PatientRecordPort repo;
    private final UserPort userRepo;

    @Override
    public List<PatientRecordResponse> forPatient(Long patientId) {
        UserEntity patient = userRepo.findById(patientId).orElseThrow();
        return repo.findByPatient(patient).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<PatientRecordResponse> forDentist(Long dentistId) {
        UserEntity dentist = userRepo.findById(dentistId).orElseThrow();
        return repo.findByDentist(dentist).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PatientRecordResponse create(Long patientId, PatientRecordRequest req) {
        UserEntity patient = userRepo.findById(patientId).orElseThrow();
        UserEntity dentist = req.getDentistId() != null ? userRepo.findById(req.getDentistId()).orElseThrow() : null;
        PatientRecord r = PatientRecord.builder()
                .patient(patient)
                .diagnosis(req.getDiagnosis())
                .treatmentPlan(req.getTreatmentPlan())
                .dentist(dentist)
                .build();
        PatientRecord saved = repo.save(r);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PatientRecordResponse update(Long id, PatientRecordRequest req) {
        PatientRecord rec = repo.findById(id).orElseThrow();
        if (req.getDiagnosis() != null) rec.setDiagnosis(req.getDiagnosis());
        if (req.getTreatmentPlan() != null) rec.setTreatmentPlan(req.getTreatmentPlan());
        PatientRecord saved = repo.save(rec);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private PatientRecordResponse toResponse(PatientRecord r) {
        PatientRecordResponse resp = new PatientRecordResponse();
        resp.setId(r.getId());
        resp.setPatientId(r.getPatient() != null ? r.getPatient().getId() : null);
        resp.setDentistId(r.getDentist() != null ? r.getDentist().getId() : null);
        resp.setDiagnosis(r.getDiagnosis());
        resp.setTreatmentPlan(r.getTreatmentPlan());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}

