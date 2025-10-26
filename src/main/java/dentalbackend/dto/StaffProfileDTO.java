package dentalbackend.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffProfileDTO {
    public Long id;
    public Long userId;
    public String code;
    public String nickname;
    public String companyEmail;
    public Long departmentId;
    public Long branchId;
    public LocalDate birthDate;
    public String gender;
    public String phone;
}

