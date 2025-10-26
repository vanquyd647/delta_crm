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

    /** Keep a transient enum-compatible field for builder and existing callers. This will be synchronized with roleEntity. */
    @Transient
    private UserRole role;

    /** Replace previous enum role field with a proper Role entity relation. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role roleEntity;

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

    @Column(length = 512)
    private String avatarUrl; // store user's avatar/profile image URL (e.g. from Google)

    /** Synchronize transient role and roleEntity after loading from DB */
    @PostLoad
    private void postLoadSyncRole() {
        if (this.roleEntity != null) {
            this.role = this.roleEntity.getName();
        }
    }

    /**
     * Single lifecycle callback for both persist and update.
     * - Ensure timestamps (createdAt/updatedAt) are set.
     * - Do NOT create transient Role entity here; service layer must resolve roleEntity to avoid duplicate inserts.
     */
    @PrePersist
    @PreUpdate
    private void beforeSave() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;

        // Do not auto-create roleEntity from `role` here; that causes cascade insert and duplicates.
        if (this.roleEntity != null && this.role == null) {
            this.role = this.roleEntity.getName();
        }
    }

    /** Compatibility: keep previous getRole()/setRole(UserRole) methods so existing code that reads/writes enum continues to work */
    public UserRole getRole() {
        if (this.role != null) return this.role;
        return roleEntity != null ? roleEntity.getName() : null;
    }

    public void setRole(UserRole role) {
        // Only set transient enum; do not create roleEntity here.
        this.role = role;
    }

    // Allow direct access to the Role entity when needed
    public Role getRoleEntity() { return this.roleEntity; }
    public void setRoleEntity(Role roleEntity) { this.roleEntity = roleEntity; }

    /** Map sang Spring Security UserDetails (dao auth) */
    public UserDetails toUserDetails() {
        UserRole r = getRole();
        String roleName = r != null ? r.name() : "UNKNOWN";
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
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

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
