package dentalbackend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateDentistRequest {
    @NotBlank
    @Size(max = 128)
    private String name;

    private Long userId; // optional link to UserEntity

    @Size(max = 128)
    private String specialization;

    @Size(max = 256)
    private String email;

    @Size(max = 64)
    private String phone;

    private Boolean active = true;

    @Size(max = 2000)
    private String bio;
}

