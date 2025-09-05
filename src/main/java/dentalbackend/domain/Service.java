package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(nullable = false)
    private Double price;

    @Column(length = 1024)
    private String description;

    // Thời gian thực hiện (phút)
    @Column(nullable = false)
    private Integer durationMinutes;

    // Có thể thêm các trường khác như: isActive, promotion, etc. nếu cần
}

