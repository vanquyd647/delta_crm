package dentalbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentistResponse {
    private Long id;
    private String name;
    private Long userId;
    private String specialization;
    private String email;
    private String phone;
    private Boolean active;
    private String bio;
}

