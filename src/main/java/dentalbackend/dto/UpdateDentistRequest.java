package dentalbackend.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UpdateDentistRequest {
    @Size(max = 128)
    private String name;

    private Long userId;

    @Size(max = 128)
    private String specialization;

    @Size(max = 256)
    private String email;

    @Size(max = 64)
    private String phone;

    private Boolean active;

    @Size(max = 2000)
    private String bio;
}

