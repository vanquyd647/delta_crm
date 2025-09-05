package dentalbackend.dto;

import lombok.Data;

@Data
public class PatientRecordRequest {
    private Long dentistId;
    private String diagnosis;
    private String treatmentPlan;
}

