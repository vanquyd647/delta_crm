package dentalbackend.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class PatientRecordResponse {
    private Long id;
    private Long patientId;
    private Long dentistId;
    private String diagnosis;
    private String treatmentPlan;
    private Instant createdAt;
}

