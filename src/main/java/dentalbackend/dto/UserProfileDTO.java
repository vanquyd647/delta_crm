package dentalbackend.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileDTO {
    public Long id;
    public Long userId;
    public String phone;
    public LocalDate birthDate;
    public String gender;
    public String address;
    public String avatarUrl;
    public String emergencyContact;

    public Long sourceId;
    public String sourceDetail;
    public Long branchId;
    public Long nationalityId;
    public Long occupationId;
    public String province;
    public String district;
    public String ward;
    public Boolean isReturning;
    public Long referrerId;
    public Set<Long> customerGroupIds;
}

