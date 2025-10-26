package dentalbackend.controller;

import dentalbackend.domain.Branch;
import dentalbackend.repository.BranchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchRepository branchRepository;

    public BranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public List<Branch> list() { return branchRepository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> get(@PathVariable Long id) {
        return branchRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Branch> create(@RequestBody Branch b) {
        return ResponseEntity.ok(branchRepository.save(b));
    }
}

