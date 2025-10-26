package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 64)
    private String phone;

    @Column(length = 255)
    private String representative;

    @Column(length = 64)
    private String bankCode;

    @Column(length = 128)
    private String bankAccount;

    private Double deposit;

    @Column(length = 255)
    private String email;

    @Column(length = 1024)
    private String laboTemplate;

    @Column(length = 512)
    private String address;
}
