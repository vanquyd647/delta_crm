package dentalbackend.controller;

import dentalbackend.application.user.UserUseCase;
import dentalbackend.common.ApiResponse;
import dentalbackend.domain.StaffProfile;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.UserRole;
import dentalbackend.repository.BranchRepository;
import dentalbackend.repository.DepartmentRepository;
import dentalbackend.repository.StaffProfileRepository;
import dentalbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/staffs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {
    private static final Logger log = LoggerFactory.getLogger(AdminStaffController.class);
    private final UserUseCase userUseCase;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list() {
        try {
            List<UserEntity> all = userRepository.findAll();
            List<Map<String,Object>> out = new ArrayList<>();
            for (UserEntity u : all) {
                try {
                    if (u.getRole() == UserRole.CUSTOMER) continue; // skip customers
                } catch (Exception ignored) {}
                Map<String,Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                m.put("email", u.getEmail());
                m.put("fullName", u.getFullName());
                m.put("role", u.getRole() != null ? u.getRole().name() : null);
                m.put("enabled", u.isEnabled());
                // attach staff profile id if exists
                try {
                    StaffProfile sp = staffProfileRepository.findByUserId(u.getId());
                    if (sp != null) {
                        m.put("staffProfileId", sp.getId());
                        m.put("staffCode", sp.getCode());
                        m.put("staffNickname", sp.getNickname());
                    }
                } catch (Exception ignored) {}
                out.add(m);
            }
            return ApiResponse.ok(out);
        } catch (Exception ex) {
            // Log the error to server logs for debugging and return message to client
            log.error("AdminStaffController.list failed", ex);
            return ApiResponse.error("Internal server error: " + ex.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> payload) {
        // expected fields: username, email, password, role, fullName, createProfile (bool), profile fields
        String username = (String) payload.get("username");
        String email = (String) payload.get("email");
        String password = (String) payload.get("password");
        String roleStr = (String) payload.getOrDefault("role", "RECEPTIONIST");
        String fullName = (String) payload.get("fullName");
        boolean createProfile = Boolean.TRUE.equals(payload.get("createProfile"));

        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.error("username, email and password are required");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception ex) {
            return ApiResponse.error("Invalid role: " + roleStr);
        }

        // create user via useCase to ensure password hashing and profile creation
        UserEntity created;
        try {
            created = userUseCase.createUser(username, email, password, role);
            if (fullName != null) { created.setFullName(fullName); userUseCase.save(created); }
        } catch (Exception e) {
            return ApiResponse.error("Failed to create user: " + e.getMessage());
        }

        Map<String,Object> result = new HashMap<>();
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("id", created.getId());
        userMap.put("username", created.getUsername());
        userMap.put("email", created.getEmail());
        userMap.put("role", created.getRole()!=null?created.getRole().name():null);
        result.put("user", userMap);

        // If createProfile requested, but a staff profile already exists for this user, return existing info instead of creating a duplicate
        if (createProfile) {
            StaffProfile existing = staffProfileRepository.findByUserId(created.getId());
            if (existing != null) {
                result.put("staffProfileId", existing.getId());
                result.put("staffProfileAlreadyExists", true);
                return ApiResponse.ok("User created; staff profile already exists", result);
            }
        }

        if (createProfile) {
            try {
                StaffProfile p = new StaffProfile();
                p.setUser(created);
                p.setCode((String) payload.get("code"));
                p.setNickname((String) payload.get("nickname"));
                p.setCompanyEmail((String) payload.get("companyEmail"));
                Object deptId = payload.get("departmentId");
                if (deptId != null) {
                    Long did = Long.valueOf(String.valueOf(deptId));
                    departmentRepository.findById(did).ifPresent(p::setDepartment);
                }
                Object branchId = payload.get("branchId");
                if (branchId != null) {
                    Long bid = Long.valueOf(String.valueOf(branchId));
                    branchRepository.findById(bid).ifPresent(p::setBranch);
                }
                p.setPhone((String) payload.get("phone"));
                Object bd = payload.get("birthDate");
                if (bd != null) {
                    try { p.setBirthDate(LocalDate.parse(String.valueOf(bd))); } catch (Exception ignored) {}
                }
                StaffProfile saved = staffProfileRepository.save(p);
                result.put("staffProfileId", saved.getId());
            } catch (Exception e) {
                // created user but profile failed
                result.put("staffProfileError", e.getMessage());
            }
        }

        return ApiResponse.ok("Staff created", result);
    }

    @PutMapping("/{userId}")
    public ApiResponse<?> update(@PathVariable Long userId, @RequestBody Map<String,Object> payload) {
        var userOpt = userUseCase.findById(userId);
        if (userOpt.isEmpty()) return ApiResponse.error("User not found");
        var user = userOpt.get();
        // update basic fields
        if (payload.containsKey("username")) user.setUsername(String.valueOf(payload.get("username")));
        if (payload.containsKey("email")) user.setEmail(String.valueOf(payload.get("email")));
        if (payload.containsKey("fullName")) user.setFullName(String.valueOf(payload.get("fullName")));
        if (payload.containsKey("enabled")) user.setEnabled(Boolean.parseBoolean(String.valueOf(payload.get("enabled"))));
        if (payload.containsKey("role")) {
            try { user.setRole(dentalbackend.domain.UserRole.valueOf(String.valueOf(payload.get("role")))); } catch (Exception ignored) {}
        }
        var saved = userUseCase.save(user);

        Map<String,Object> result = new HashMap<>();
        result.put("user", Map.of("id", saved.getId(), "username", saved.getUsername(), "email", saved.getEmail(), "role", saved.getRole()!=null?saved.getRole().name():null));

        // update or create staff profile fields
        try {
            StaffProfile sp = staffProfileRepository.findByUserId(saved.getId());
            boolean createIfMissing = Boolean.TRUE.equals(payload.get("createProfile"));
            if (sp == null && createIfMissing) {
                sp = new StaffProfile();
                sp.setUser(saved);
            }
            if (sp != null) {
                if (payload.containsKey("code")) sp.setCode(String.valueOf(payload.get("code")));
                if (payload.containsKey("nickname")) sp.setNickname(String.valueOf(payload.get("nickname")));
                if (payload.containsKey("companyEmail")) sp.setCompanyEmail(String.valueOf(payload.get("companyEmail")));
                if (payload.containsKey("phone")) sp.setPhone(String.valueOf(payload.get("phone")));
                Object deptId = payload.get("departmentId");
                if (deptId != null) {
                    Long did = Long.valueOf(String.valueOf(deptId));
                    departmentRepository.findById(did).ifPresent(sp::setDepartment);
                }
                Object branchId = payload.get("branchId");
                if (branchId != null) {
                    Long bid = Long.valueOf(String.valueOf(branchId));
                    branchRepository.findById(bid).ifPresent(sp::setBranch);
                }
                StaffProfile savedSp = staffProfileRepository.save(sp);
                result.put("staffProfileId", savedSp.getId());
            }
        } catch (Exception ex) {
            // ignore
        }

        return ApiResponse.ok("Updated", result);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<?> delete(@PathVariable Long userId, @RequestParam(name = "deleteProfileOnly", required = false, defaultValue = "false") boolean deleteProfileOnly) {
        // if deleteProfileOnly true, only remove staff profile
        try {
            StaffProfile sp = staffProfileRepository.findByUserId(userId);
            if (sp != null) staffProfileRepository.delete(sp);
            if (deleteProfileOnly) return ApiResponse.ok("Staff profile deleted", null);
            // delete user
            userUseCase.deleteUser(userId);
            return ApiResponse.ok("User (and staff profile if existed) deleted", null);
        } catch (Exception ex) {
            return ApiResponse.error("Delete failed: " + ex.getMessage());
        }
    }
}
