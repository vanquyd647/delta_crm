package dentalbackend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class UpdateServiceRequest {
    @NotBlank
    @Size(max = 128)
    private String name;

    @NotNull
    @Positive
    private Double price;

    @Size(max = 1024)
    private String description;

    @NotNull
    @Positive
    private Integer durationMinutes;
}

