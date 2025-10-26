package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "user_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private UserGroup parentGroup;

    @Column(length = 1024)
    private String notes;

    @Column(length = 128)
    private String timeOff;

    private Integer mandatoryPasswordChangeDays;

    private Boolean notifyGroup;

    @ManyToMany
    @JoinTable(name = "user_group_members",
            joinColumns = @JoinColumn(name = "user_group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<UserEntity> members;
}
