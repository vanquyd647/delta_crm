package dentalbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationRequest {
    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    /** e.g. Zalo, Hotline, Email, Phone */
    @NotBlank
    @Size(max = 50)
    private String method;

    @Size(max = 2000)
    private String content;
}
