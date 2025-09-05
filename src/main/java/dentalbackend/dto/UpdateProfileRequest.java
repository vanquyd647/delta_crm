package dentalbackend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String phone;
    private String birthDate; // ISO date yyyy-MM-dd
    private String gender;
    private String address;
    private String avatarUrl;
    private String emergencyContact;
}

