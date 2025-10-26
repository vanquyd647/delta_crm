package dentalbackend.controller;

import dentalbackend.domain.*;
import dentalbackend.dto.UserProfileDTO;
import dentalbackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final SourceRepository sourceRepository;
    private final BranchRepository branchRepository;
    private final NationalityRepository nationalityRepository;
    private final OccupationRepository occupationRepository;
    private final CustomerGroupRepository customerGroupRepository;

    public UserProfileController(UserProfileRepository userProfileRepository,
                                 UserRepository userRepository,
                                 SourceRepository sourceRepository,
                                 BranchRepository branchRepository,
                                 NationalityRepository nationalityRepository,
                                 OccupationRepository occupationRepository,
                                 CustomerGroupRepository customerGroupRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.sourceRepository = sourceRepository;
        this.branchRepository = branchRepository;
        this.nationalityRepository = nationalityRepository;
        this.occupationRepository = occupationRepository;
        this.customerGroupRepository = customerGroupRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> get(@PathVariable Long id) {
        return userProfileRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserProfileDTO> create(@RequestBody UserProfileDTO dto) {
        Optional<UserEntity> userOpt = userRepository.findById(dto.userId);
        if (userOpt.isEmpty()) return ResponseEntity.badRequest().build();

        UserProfile p = new UserProfile();
        p.setUser(userOpt.get());
        p.setPhone(dto.phone);
        p.setBirthDate(dto.birthDate);
        p.setGender(dto.gender);
        p.setAddress(dto.address);
        p.setAvatarUrl(dto.avatarUrl);
        p.setEmergencyContact(dto.emergencyContact);
        p.setSource(dto.sourceId != null ? sourceRepository.findById(dto.sourceId).orElse(null) : null);
        p.setSourceDetail(dto.sourceDetail);
        p.setBranch(dto.branchId != null ? branchRepository.findById(dto.branchId).orElse(null) : null);
        p.setNationality(dto.nationalityId != null ? nationalityRepository.findById(dto.nationalityId).orElse(null) : null);
        p.setOccupation(dto.occupationId != null ? occupationRepository.findById(dto.occupationId).orElse(null) : null);
        p.setProvince(dto.province);
        p.setDistrict(dto.district);
        p.setWard(dto.ward);
        p.setIsReturning(dto.isReturning != null ? dto.isReturning : Boolean.FALSE);
        p.setReferrer(dto.referrerId != null ? userRepository.findById(dto.referrerId).orElse(null) : null);

        if (dto.customerGroupIds != null && !dto.customerGroupIds.isEmpty()) {
            Set<CustomerGroup> groups = dto.customerGroupIds.stream()
                    .map(customerGroupRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            p.setCustomerGroups(groups);
        }

        UserProfile saved = userProfileRepository.save(p);
        return ResponseEntity.ok(toDto(saved));
    }

    // Simple list endpoint for UI (patients) to populate selects
    @GetMapping("/list")
    public ResponseEntity<List<Map<String,Object>>> listAll() {
        var list = userProfileRepository.findAll();
        List<Map<String,Object>> out = list.stream().map(p -> {
            Map<String,Object> m = new java.util.HashMap<>();
            m.put("id", p.getUser()!=null? p.getUser().getId() : p.getId());
            m.put("name", p.getUser()!=null? p.getUser().getFullName() : (p.getPhone()!=null? p.getPhone():"#"+p.getId()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    private UserProfileDTO toDto(UserProfile p) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.id = p.getId();
        dto.userId = p.getUser() != null ? p.getUser().getId() : null;
        dto.phone = p.getPhone();
        dto.birthDate = p.getBirthDate();
        dto.gender = p.getGender();
        dto.address = p.getAddress();
        dto.avatarUrl = p.getAvatarUrl();
        dto.emergencyContact = p.getEmergencyContact();
        dto.sourceId = p.getSource() != null ? p.getSource().getId() : null;
        dto.sourceDetail = p.getSourceDetail();
        dto.branchId = p.getBranch() != null ? p.getBranch().getId() : null;
        dto.nationalityId = p.getNationality() != null ? p.getNationality().getId() : null;
        dto.occupationId = p.getOccupation() != null ? p.getOccupation().getId() : null;
        dto.province = p.getProvince();
        dto.district = p.getDistrict();
        dto.ward = p.getWard();
        dto.isReturning = p.getIsReturning();
        dto.referrerId = p.getReferrer() != null ? p.getReferrer().getId() : null;
        dto.customerGroupIds = p.getCustomerGroups() != null ? p.getCustomerGroups().stream().map(CustomerGroup::getId).collect(Collectors.toSet()) : new HashSet<>();
        return dto;
    }
}
