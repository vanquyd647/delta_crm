package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 1024)
    private String notes;
}
