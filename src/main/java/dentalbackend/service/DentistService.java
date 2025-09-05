package dentalbackend.application.dentist.impl;

import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.dto.CreateDentistRequest;
import dentalbackend.dto.DentistResponse;
import dentalbackend.dto.UpdateDentistRequest;
import dentalbackend.domain.Dentist;
import dentalbackend.domain.port.DentistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DentistService implements DentistUseCase {
    private final DentistPort repo;

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
        Dentist d = new Dentist();
        d.setName(req.getName());
        d.setUserId(req.getUserId());
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
}
