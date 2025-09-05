package dentalbackend.dto;

import lombok.Data;

@Data
public class ServiceResponse {
    private Long id;
    private String name;
    private Double price;
    private String description;
    private Integer durationMinutes;
}

