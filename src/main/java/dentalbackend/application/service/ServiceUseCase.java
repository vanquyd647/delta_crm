package dentalbackend.application.service;

import dentalbackend.dto.ServiceResponse;
import dentalbackend.dto.CreateServiceRequest;
import dentalbackend.dto.UpdateServiceRequest;

import java.util.List;

public interface ServiceUseCase {
    List<ServiceResponse> listAll();
    ServiceResponse getById(Long id);
    ServiceResponse create(CreateServiceRequest req);
    ServiceResponse update(Long id, UpdateServiceRequest req);
    void delete(Long id);
}

