package dentalbackend.dto;

import lombok.Data;

@Data
public class CompleteAppointmentRequest {
    // Optional clinical record to create when completing the appointment
    private String diagnosis;
    private String treatmentPlan;
}

