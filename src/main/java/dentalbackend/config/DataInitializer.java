package dentalbackend.config;

import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.domain.Role;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserUseCase userUseCase;
    private final RoleRepository roleRepository;

    @Value("${app.bootstrap.create-admin:true}")
    private boolean createAdmin;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:ChangeMe123!}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Ensure roles exist (idempotent, tolerant to races/duplicates)
        for (UserRole r : UserRole.values()) {
            try {
                roleRepository.findByName(r).ifPresentOrElse(existing -> {
                    // OK, already present
                }, () -> {
                    try {
                        Role role = Role.builder().name(r).build();
                        roleRepository.save(role);
                        log.info("Inserted missing role: {}", r);
                    } catch (DataIntegrityViolationException dive) {
                        // Another process/thread inserted concurrently; ignore
                        log.debug("Role {} already exists (race) - ignoring: {}", r, dive.getMessage());
                    } catch (Exception ex) {
                        log.warn("Unable to create role {} : {}", r, ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                log.warn("Unable to ensure role {} exists: {}", r, ex.getMessage());
            }
        }

        if (!createAdmin) return;

        try {
            // If admin user already exists, skip
            var existing = userUseCase.findByUsernameOrEmail(adminUsername).orElse(null);
            if (existing != null) {
                log.info("Admin user already exists: {} (id={})", existing.getUsername(), existing.getId());
                return;
            }

            // Create admin user
            UserEntity admin = userUseCase.createUser(adminUsername, adminEmail, adminPassword, UserRole.ADMIN);
            log.info("Initial admin created: {} (id={})", admin.getUsername(), admin.getId());
        } catch (Exception ex) {
            log.error("Failed to create bootstrap admin: {}", ex.getMessage(), ex);
        }
    }
}
