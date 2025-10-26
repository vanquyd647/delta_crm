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

    // raw FK value (read-only) for compatibility with legacy data where dentist_id references dentists.id
    @Column(name = "dentist_id", insertable = false, updatable = false)
    private Long dentistRefId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receptionist_id")
    private UserEntity receptionist;

    // Link to Service entity for booked service
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_id")
    private Service service;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private Instant scheduledTime;

    @Column(length = 1024)
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;

    // --- New fields from V10
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_id")
    private UserEntity assistant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private Integer estimatedMinutes;

    // --- end new fields

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
