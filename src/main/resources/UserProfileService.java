package dentalbackend.service;

import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserProfile;
import dentalbackend.dto.UpdateProfileRequest;
import dentalbackend.dto.UserProfileResponse;
import dentalbackend.repository.UserProfileRepository;
import dentalbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository profileRepo;
    private final UserRepository userRepo;

    public Optional<UserProfileResponse> getByUserId(Long userId) {
        return userRepo.findById(userId).flatMap(user -> profileRepo.findByUser(user).map(this::toResponse));
    }

    @Transactional
    public UserProfileResponse update(Long userId, UpdateProfileRequest req) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        UserProfile profile = profileRepo.findByUser(user).orElseGet(() -> {
            UserProfile p = UserProfile.builder().user(user).build();
            return profileRepo.save(p);
        });

        if (req.getPhone() != null) profile.setPhone(req.getPhone());
        if (req.getAddress() != null) profile.setAddress(req.getAddress());
        if (req.getAvatarUrl() != null) profile.setAvatarUrl(req.getAvatarUrl());
        if (req.getEmergencyContact() != null) profile.setEmergencyContact(req.getEmergencyContact());
        if (req.getGender() != null) profile.setGender(req.getGender());
        if (req.getBirthDate() != null) {
            try {
                profile.setBirthDate(LocalDate.parse(req.getBirthDate()));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid birthDate format, expected yyyy-MM-dd");
            }
        }

        UserProfile saved = profileRepo.save(profile);
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        UserProfileResponse r = new UserProfileResponse();
        r.setId(p.getId());
        r.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        r.setPhone(p.getPhone());
        r.setBirthDate(p.getBirthDate());
        r.setGender(p.getGender());
        r.setAddress(p.getAddress());
        r.setAvatarUrl(p.getAvatarUrl());
        r.setEmergencyContact(p.getEmergencyContact());
        return r;
    }
}

