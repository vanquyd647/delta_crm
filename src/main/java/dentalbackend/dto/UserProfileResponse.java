package dentalbackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileResponse {
    private Long id;
    private Long userId;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String avatarUrl;
    private String emergencyContact;
}

