package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="patient_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name="patient_id")
    private UserEntity patient;

    // link to dentist who created/owns this record
    @ManyToOne @JoinColumn(name="dentist_id")
    private UserEntity dentist;

    @Column(length=2048)
    private String diagnosis;

    @Column(length=2048)
    private String treatmentPlan;

    private Instant createdAt;

    @PrePersist
    public void prePersist() { createdAt = Instant.now(); }
}
