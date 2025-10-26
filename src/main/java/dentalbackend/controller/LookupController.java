package dentalbackend.controller;

import dentalbackend.domain.*;
import dentalbackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
public class LookupController {
    private final SourceRepository sourceRepository;
    private final CustomerGroupRepository customerGroupRepository;
    private final NationalityRepository nationalityRepository;
    private final OccupationRepository occupationRepository;
    private final DepartmentRepository departmentRepository;

    public LookupController(SourceRepository sourceRepository,
                            CustomerGroupRepository customerGroupRepository,
                            NationalityRepository nationalityRepository,
                            OccupationRepository occupationRepository,
                            DepartmentRepository departmentRepository) {
        this.sourceRepository = sourceRepository;
        this.customerGroupRepository = customerGroupRepository;
        this.nationalityRepository = nationalityRepository;
        this.occupationRepository = occupationRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/sources")
    public List<Source> sources() { return sourceRepository.findAll(); }

    @PostMapping("/sources")
    public ResponseEntity<Source> createSource(@RequestBody Source s) { return ResponseEntity.ok(sourceRepository.save(s)); }

    @GetMapping("/customer-groups")
    public List<CustomerGroup> customerGroups() { return customerGroupRepository.findAll(); }

    @PostMapping("/customer-groups")
    public ResponseEntity<CustomerGroup> createGroup(@RequestBody CustomerGroup g) { return ResponseEntity.ok(customerGroupRepository.save(g)); }

    @GetMapping("/nationalities")
    public List<Nationality> nationalities() { return nationalityRepository.findAll(); }

    @PostMapping("/nationalities")
    public ResponseEntity<Nationality> createNationality(@RequestBody Nationality n) { return ResponseEntity.ok(nationalityRepository.save(n)); }

    @GetMapping("/occupations")
    public List<Occupation> occupations() { return occupationRepository.findAll(); }

    @PostMapping("/occupations")
    public ResponseEntity<Occupation> createOccupation(@RequestBody Occupation o) { return ResponseEntity.ok(occupationRepository.save(o)); }

    @GetMapping("/departments")
    public List<Department> departments() { return departmentRepository.findAll(); }

    @PostMapping("/departments")
    public ResponseEntity<Department> createDepartment(@RequestBody Department d) { return ResponseEntity.ok(departmentRepository.save(d)); }
}

