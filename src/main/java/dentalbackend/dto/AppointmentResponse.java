package dentalbackend.dto;

import dentalbackend.domain.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private AppointmentStatus status;
    private Instant scheduledTime;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private Long customerId;
    private String customerUsername;
    private String customerEmail;
    private String customerEmergencyContact;

    private Long dentistId;
    private String dentistUsername;

    private Long receptionistId;
    private String receptionistUsername;

    // Service linked to appointment (optional)
    private Long serviceId;
    private String serviceName;
}
