package dentalbackend.controller;

import dentalbackend.domain.*;
import dentalbackend.dto.StaffProfileDTO;
import dentalbackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff-profiles")
public class StaffProfileController {
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;

    public StaffProfileController(StaffProfileRepository staffProfileRepository,
                                  UserRepository userRepository,
                                  DepartmentRepository departmentRepository,
                                  BranchRepository branchRepository) {
        this.staffProfileRepository = staffProfileRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
    }

    // Simple list endpoint for UI to populate assistant/staff selects
    @GetMapping("/list")
    public ResponseEntity<List<Map<String,Object>>> list() {
        List<StaffProfile> list = staffProfileRepository.findAll();
        List<Map<String,Object>> out = list.stream().map(s -> {
            Map<String,Object> m = new java.util.HashMap<>();
            m.put("id", s.getUser() != null ? s.getUser().getId() : s.getId());
            m.put("name", s.getNickname() != null ? s.getNickname() : (s.getUser()!=null? s.getUser().getFullName():null));
            m.put("code", s.getCode());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return staffProfileRepository.findById(id)
                .map(p -> ResponseEntity.ok(dentalbackend.common.ApiResponse.ok(this.toDto(p))))
                .orElse(ResponseEntity.status(404).body(dentalbackend.common.ApiResponse.error("Staff profile not found")));
    }

    // Fetch staff profile by linked user id
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable Long userId) {
        try {
            StaffProfile sp = staffProfileRepository.findByUserId(userId);
            if (sp == null) return ResponseEntity.status(404).body(dentalbackend.common.ApiResponse.error("Staff profile not found for user"));
            return ResponseEntity.ok(dentalbackend.common.ApiResponse.ok(this.toDto(sp)));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(dentalbackend.common.ApiResponse.error("Internal error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StaffProfileDTO dto) {
        Optional<UserEntity> u = userRepository.findById(dto.userId);
        if (u.isEmpty()) return ResponseEntity.badRequest().body(dentalbackend.common.ApiResponse.error("User not found"));
        // Enforce that linked user should NOT be a CUSTOMER (staff only)
        try {
            if (u.get().getRole() == dentalbackend.domain.UserRole.CUSTOMER) {
                return ResponseEntity.badRequest().body(dentalbackend.common.ApiResponse.error("User role is CUSTOMER; cannot create staff profile"));
            }
        } catch (Exception ignored) {
            // best-effort
        }

        // Prevent duplicate staff profile
        StaffProfile existing = staffProfileRepository.findByUserId(u.get().getId());
        if (existing != null) {
            return ResponseEntity.status(409).body(dentalbackend.common.ApiResponse.error("Staff profile already exists for this user"));
        }

        StaffProfile p = new StaffProfile();
        p.setUser(u.get());
        p.setCode(dto.code);
        p.setNickname(dto.nickname);
        p.setCompanyEmail(dto.companyEmail);
        p.setDepartment(dto.departmentId != null ? departmentRepository.findById(dto.departmentId).orElse(null) : null);
        p.setBranch(dto.branchId != null ? branchRepository.findById(dto.branchId).orElse(null) : null);
        p.setBirthDate(dto.birthDate);
        p.setGender(dto.gender);
        p.setPhone(dto.phone);
        StaffProfile saved = staffProfileRepository.save(p);
        return ResponseEntity.ok(dentalbackend.common.ApiResponse.ok(this.toDto(saved)));
    }

    private StaffProfileDTO toDto(StaffProfile p) {
        StaffProfileDTO dto = new StaffProfileDTO();
        dto.id = p.getId();
        dto.userId = p.getUser() != null ? p.getUser().getId() : null;
        dto.code = p.getCode();
        dto.nickname = p.getNickname();
        dto.companyEmail = p.getCompanyEmail();
        dto.departmentId = p.getDepartment() != null ? p.getDepartment().getId() : null;
        dto.branchId = p.getBranch() != null ? p.getBranch().getId() : null;
        dto.birthDate = p.getBirthDate();
        dto.gender = p.getGender();
        dto.phone = p.getPhone();
        return dto;
    }
}
