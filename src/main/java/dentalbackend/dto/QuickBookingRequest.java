package dentalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuickBookingRequest {
    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    /** service id from system (required) */
    @NotNull
    private Long serviceId;

    /** Date string in format MM/dd/yyyy (e.g. 12/31/2025) */
    @NotBlank
    private String date;

    /** Time string in format HH:mm (24-hour, e.g. 14:30) */
    @NotBlank
    private String time;

    /** Optional preferred dentist id */
    private Long dentistId;

    @Size(max = 2000)
    private String notes;
}
