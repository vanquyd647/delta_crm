package dentalbackend.domain.port;

import dentalbackend.domain.UserEntity;

import java.util.Optional;

public interface UserPort {
    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByUsernameOrEmail(String usernameOrEmail);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
    UserEntity save(UserEntity user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void deleteById(Long id);
}

