package dentalbackend.controller;

import dentalbackend.domain.UserGroup;
import dentalbackend.dto.*;
import dentalbackend.repository.UserGroupRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-groups")
public class UserGroupController {
    private final UserGroupRepository userGroupRepository;

    public UserGroupController(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserGroup> get(@PathVariable Long id) {
        Optional<UserGroup> g = userGroupRepository.findById(id);
        return g.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<UserGroup>> list() {
        return ResponseEntity.ok(userGroupRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<UserGroup> create(@RequestBody UserGroup group) {
        UserGroup saved = userGroupRepository.save(group);
        return ResponseEntity.ok(saved);
    }
}
