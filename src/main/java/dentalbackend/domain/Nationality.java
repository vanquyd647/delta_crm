package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nationalities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Nationality {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;
}
