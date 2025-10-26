package dentalbackend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierDTO {
    public Long id;
    public String code;
    public String name;
    public String phone;
    public String representative;
    public String bankCode;
    public String bankAccount;
    public Double deposit;
    public String email;
    public String laboTemplate;
    public String address;
}

