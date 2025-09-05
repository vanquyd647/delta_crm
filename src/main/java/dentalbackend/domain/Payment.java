package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false)
    private Double amount;

    @Column(length = 32)
    private String method; // e.g. cash, card, bank

    @Column(length = 64)
    private String invoiceNumber;

    private Instant paidAt;

    @PrePersist
    public void prePersist() {
        paidAt = Instant.now();
    }
}

