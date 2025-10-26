package dentalbackend.controller;

import dentalbackend.domain.Discount;
import dentalbackend.repository.DiscountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {
    private final DiscountRepository discountRepository;

    public DiscountController(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @GetMapping
    public List<Discount> list() { return discountRepository.findAll(); }

    @PostMapping
    public ResponseEntity<Discount> create(@RequestBody Discount d) { return ResponseEntity.ok(discountRepository.save(d)); }
}

