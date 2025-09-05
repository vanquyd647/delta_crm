package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dentists")
@Getter @Setter
public class Dentist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MariaDB/MySQL
    private Long id;

    @Column(nullable = false)
    private String name;

    // Link to application user (optional) - stores UserEntity.id when dentist is also a user
    @Column(name = "user_id")
    private Long userId;

    @Column
    private String specialization;

    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private Boolean active = true;

    @Column(length = 2000)
    private String bio;

    // other columns can be added later (working hours, ratings, etc.)
}
