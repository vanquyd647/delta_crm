package dentalbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentRequest {
    private Instant scheduledTime;
    private String notes;
    private Long dentistId;  // ID from dentists table
    private Long serviceId;  // ID from services table
}
