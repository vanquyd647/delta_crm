package dentalbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateAppointmentRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private Long dentistId;
    @NotNull @Future
    private Instant scheduledTime;
    private String notes;
}
