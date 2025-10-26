package dentalbackend.repository;

import dentalbackend.domain.Role;
import dentalbackend.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
    boolean existsByName(UserRole name);
}

