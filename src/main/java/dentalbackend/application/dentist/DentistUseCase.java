package dentalbackend.application.dentist;

import dentalbackend.dto.DentistResponse;
import dentalbackend.dto.CreateDentistRequest;
import dentalbackend.dto.UpdateDentistRequest;

import java.util.List;

public interface DentistUseCase {
    List<DentistResponse> listActive();
    List<DentistResponse> listAll();
    DentistResponse getById(Long id);
    DentistResponse create(CreateDentistRequest req);
    DentistResponse update(Long id, UpdateDentistRequest req);
    void delete(Long id);
    DentistResponse findByUserId(Long userId);
}

