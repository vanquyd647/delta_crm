package dentalbackend.application.service.impl;

import dentalbackend.application.service.ServiceUseCase;
import dentalbackend.dto.CreateServiceRequest;
import dentalbackend.dto.UpdateServiceRequest;
import dentalbackend.dto.ServiceResponse;
import dentalbackend.domain.Service;
import dentalbackend.domain.port.ServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service("applicationServiceService")
@Primary
@RequiredArgsConstructor
public class ServiceService implements ServiceUseCase {
    private final ServicePort repo;

    @Override
    public List<ServiceResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public ServiceResponse getById(Long id) {
        return repo.findById(id).map(this::toResponse).orElse(null);
    }

    @Transactional
    @Override
    public ServiceResponse create(CreateServiceRequest req) {
        if (repo.existsByName(req.getName())) throw new IllegalArgumentException("Service name already exists");
        Service service = Service.builder()
                .name(req.getName())
                .price(req.getPrice())
                .description(req.getDescription())
                .durationMinutes(req.getDurationMinutes())
                .build();
        return toResponse(repo.save(service));
    }

    @Transactional
    @Override
    public ServiceResponse update(Long id, UpdateServiceRequest req) {
        Service service = repo.findById(id).orElseThrow();
        service.setName(req.getName());
        service.setPrice(req.getPrice());
        service.setDescription(req.getDescription());
        service.setDurationMinutes(req.getDurationMinutes());
        return toResponse(repo.save(service));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private ServiceResponse toResponse(Service service) {
        ServiceResponse resp = new ServiceResponse();
        resp.setId(service.getId());
        resp.setName(service.getName());
        resp.setPrice(service.getPrice());
        resp.setDescription(service.getDescription());
        resp.setDurationMinutes(service.getDurationMinutes());
        return resp;
    }
}
