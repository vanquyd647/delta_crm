package dentalbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String username;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    /** BCrypt hash cho login LOCAL. Với tài khoản tạo từ Google, dùng chuỗi random đã băm để không null. */
    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    /** Thông tin liên kết OAuth2/OIDC */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AuthProvider provider;      // LOCAL hoặc GOOGLE

    @Column(length = 128)
    private String providerId;          // "sub" (subject) từ Google

    @Column(length = 255)
    private String fullName;            // tên hiển thị (lấy từ Google hoặc lúc đăng ký)

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean enabled;

    /** Tuỳ chọn cá nhân hoá (đã có trong project của bạn) */
    @Column(length = 32)
    private String themePreference;     // "light" / "dark"

    @Column(length = 8)
    private String languagePreference;  // "vi", "en", ...

    @Column(length = 32)
    private String notificationPreference; // "EMAIL", "NONE", ...

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Double debt;

    @Column(length = 256)
    private String serviceStatus;

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

    /** Map sang Spring Security UserDetails (dao auth) */
    public UserDetails toUserDetails() {
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return new org.springframework.security.core.userdetails.User(
                this.username,
                this.passwordHash,
                this.enabled,
                true,
                true,
                true,
                authorities
        );
    }
}
