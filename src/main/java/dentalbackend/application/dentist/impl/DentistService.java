package dentalbackend.application.dentist.impl;

import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.dto.CreateDentistRequest;
import dentalbackend.dto.DentistResponse;
import dentalbackend.dto.UpdateDentistRequest;
import dentalbackend.domain.Dentist;
import dentalbackend.domain.port.DentistPort;
import dentalbackend.domain.port.UserPort;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.domain.AuthProvider;
import dentalbackend.domain.Role;
import dentalbackend.infrastructure.email.EmailService;
import dentalbackend.repository.RoleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DentistService implements DentistUseCase {
    private final DentistPort repo;
    private final UserPort userPort;
    private final EmailService emailService;
    private final dentalbackend.repository.UserProfileRepository profileRepo;
    private final dentalbackend.repository.UserPreferencesRepository preferencesRepo;
    private final RoleRepository roleRepository;

    @Override
    public List<DentistResponse> listActive() {
        return repo.findByActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DentistResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public DentistResponse getById(Long id) {
        return repo.findById(id).map(this::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public DentistResponse create(CreateDentistRequest req) {
        // Link or create user for dentist
        Long linkedUserId = null;
        if (req.getUserId() != null) {
            // If caller provided userId, ensure it exists
            UserEntity u = userPort.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getUserId()));
            linkedUserId = u.getId();
            // ensure role is DENTIST
            if (u.getRole() != UserRole.DENTIST) {
                u.setRole(UserRole.DENTIST);
                userPort.save(u);
            }
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            // Try find existing user by email
            var existing = userPort.findByEmail(req.getEmail());
            if (existing.isPresent()) {
                UserEntity u = existing.get();
                linkedUserId = u.getId();
                if (u.getRole() != UserRole.DENTIST) {
                    u.setRole(UserRole.DENTIST);
                    userPort.save(u);
                }
            } else {
                // create a new user account for dentist
                String username = generateUsernameFromEmail(req.getEmail());
                UserEntity newUser = UserEntity.builder()
                        .username(username)
                        .email(req.getEmail())
                        .passwordHash(UUID.randomUUID().toString())
                        .role(UserRole.DENTIST)
                        .provider(AuthProvider.LOCAL)
                        .fullName(req.getName())
                        .emailVerified(false)
                        .enabled(true)
                        .build();
                try {
                    // Resolve role entity and attach if exists (prevents duplicate inserts)
                    try {
                        Role roleEntity = roleRepository.findByName(UserRole.DENTIST).orElse(null);
                        newUser.setRoleEntity(roleEntity);
                    } catch (Exception ex) {
                        // ignore resolution errors
                    }

                    UserEntity saved = userPort.save(newUser);
                    linkedUserId = saved.getId();

                    // Ensure profile exists (best-effort)
                    try {
                        profileRepo.findByUser(saved).ifPresentOrElse(p -> {
                            if ((p.getAvatarUrl() == null || p.getAvatarUrl().isBlank()) && saved.getAvatarUrl() != null && !saved.getAvatarUrl().isBlank()) {
                                p.setAvatarUrl(saved.getAvatarUrl());
                                profileRepo.save(p);
                            }
                        }, () -> {
                            dentalbackend.domain.UserProfile profile = dentalbackend.domain.UserProfile.builder()
                                    .user(saved)
                                    .phone(null)
                                    .birthDate(null)
                                    .gender(null)
                                    .address(null)
                                    .avatarUrl(saved.getAvatarUrl())
                                    .emergencyContact(null)
                                    .build();
                            profileRepo.save(profile);
                        });
                    } catch (Exception ex) {
                        // best-effort, do not fail dentist creation
                    }

                    // Ensure preferences exist (best-effort)
                    try {
                        preferencesRepo.findByUser(saved).ifPresentOrElse(pref -> {
                            // nothing
                        }, () -> {
                            dentalbackend.domain.UserPreferences pref = dentalbackend.domain.UserPreferences.builder()
                                    .user(saved)
                                    .themePreference(saved.getThemePreference() != null ? saved.getThemePreference() : "light")
                                    .languagePreference(saved.getLanguagePreference() != null ? saved.getLanguagePreference() : "vi")
                                    .notificationPreference(saved.getNotificationPreference() != null ? saved.getNotificationPreference() : "EMAIL")
                                    .timezone(null)
                                    .build();
                            preferencesRepo.save(pref);
                        });
                    } catch (Exception ex) {
                        // best-effort
                    }

                    // send notification to dentist (welcome)
                    try {
                        String subject = "Welcome to Clinic - account created";
                        String body = "Hello " + req.getName() + ",\n\nA user account has been created for you. Email: " + req.getEmail() + "\nPlease set your password via the admin portal.\n";
                        emailService.send(req.getEmail(), subject, body);
                    } catch (Exception ex) {
                        // don't fail creation if email sending fails
                    }
                } catch (Exception ex) {
                    // fallback: leave linkedUserId null and continue
                    linkedUserId = null;
                }
            }
        }

        Dentist d = new Dentist();
        d.setName(req.getName());
        d.setUserId(linkedUserId);
        d.setSpecialization(req.getSpecialization());
        d.setEmail(req.getEmail());
        d.setPhone(req.getPhone());
        d.setActive(req.getActive() == null ? true : req.getActive());
        d.setBio(req.getBio());
        Dentist saved = repo.save(d);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DentistResponse update(Long id, UpdateDentistRequest req) {
        Dentist d = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Dentist not found: " + id));
        if (req.getName() != null) d.setName(req.getName());
        if (req.getUserId() != null) d.setUserId(req.getUserId());
        if (req.getSpecialization() != null) d.setSpecialization(req.getSpecialization());
        if (req.getEmail() != null) d.setEmail(req.getEmail());
        if (req.getPhone() != null) d.setPhone(req.getPhone());
        if (req.getActive() != null) d.setActive(req.getActive());
        if (req.getBio() != null) d.setBio(req.getBio());
        Dentist saved = repo.save(d);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    public DentistResponse findByUserId(Long userId) {
        return repo.findByUserId(userId).map(this::toResponse).orElse(null);
    }

    private DentistResponse toResponse(Dentist d) {
        return DentistResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .userId(d.getUserId())
                .specialization(d.getSpecialization())
                .email(d.getEmail())
                .phone(d.getPhone())
                .active(d.getActive())
                .bio(d.getBio())
                .build();
    }

    private String generateUsernameFromEmail(String email) {
        String local = email.split("@")[0];
        String suffix = UUID.randomUUID().toString().substring(0,6);
        String candidate = (local + "_" + suffix).replaceAll("[^A-Za-z0-9_\\.-]", "");
        if (candidate.length() > 64) candidate = candidate.substring(0,64);
        int attempt = 0;
        String finalUsername = candidate;
        while (userPort.existsByUsername(finalUsername)) {
            attempt++;
            finalUsername = candidate + attempt;
            if (attempt > 10) break;
        }
        return finalUsername;
    }
}
