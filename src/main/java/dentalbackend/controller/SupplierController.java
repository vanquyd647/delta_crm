package dentalbackend.controller;

import dentalbackend.domain.Supplier;
import dentalbackend.dto.SupplierDTO;
import dentalbackend.repository.SupplierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierRepository supplierRepository;

    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> get(@PathVariable Long id) {
        return supplierRepository.findById(id)
                .map(s -> {
                    SupplierDTO dto = SupplierDTO.builder()
                            .id(s.getId())
                            .code(s.getCode())
                            .name(s.getName())
                            .phone(s.getPhone())
                            .representative(s.getRepresentative())
                            .bankCode(s.getBankCode())
                            .bankAccount(s.getBankAccount())
                            .deposit(s.getDeposit())
                            .email(s.getEmail())
                            .laboTemplate(s.getLaboTemplate())
                            .address(s.getAddress())
                            .build();
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Supplier>> list() {
        return ResponseEntity.ok(supplierRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> create(@RequestBody SupplierDTO dto) {
        Supplier s = Supplier.builder()
                .code(dto.code)
                .name(dto.name)
                .phone(dto.phone)
                .representative(dto.representative)
                .bankCode(dto.bankCode)
                .bankAccount(dto.bankAccount)
                .deposit(dto.deposit)
                .email(dto.email)
                .laboTemplate(dto.laboTemplate)
                .address(dto.address)
                .build();
        Supplier saved = supplierRepository.save(s);
        dto.id = saved.getId();
        return ResponseEntity.ok(dto);
    }
}
