package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profiles_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 32)
    private String phone;

    private LocalDate birthDate;

    @Column(length = 16)
    private String gender;

    @Column(length = 512)
    private String address;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 255)
    private String emergencyContact;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // --- New fields added by migration V10 ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private Source source;

    @Column(length = 255)
    private String sourceDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nationality_id")
    private Nationality nationality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupation_id")
    private Occupation occupation;

    @Column(length = 128)
    private String province;

    @Column(length = 128)
    private String district;

    @Column(length = 128)
    private String ward;

    @Column(name = "is_returning")
    private Boolean isReturning = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id")
    private UserEntity referrer;

    @ManyToMany
    @JoinTable(name = "user_profile_customer_groups",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_group_id"))
    private Set<CustomerGroup> customerGroups;

    // --- end new fields ---

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
