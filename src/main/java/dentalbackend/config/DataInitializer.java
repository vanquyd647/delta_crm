package dentalbackend.config;

import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.application.user.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserUseCase userUseCase;

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
