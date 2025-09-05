package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name="idx_appt_dentist_time", columnList = "dentist_id, scheduledTime")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")
    private UserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dentist_id")
    private UserEntity dentist;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receptionist_id")
    private UserEntity receptionist;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private Instant scheduledTime;

    @Column(length = 1024)
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) status = AppointmentStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

