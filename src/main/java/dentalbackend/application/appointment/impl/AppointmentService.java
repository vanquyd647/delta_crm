package dentalbackend.application.appointment.impl;

import dentalbackend.application.appointment.AppointmentUseCase;
import dentalbackend.application.patient.PatientRecordUseCase;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.AppointmentStatus;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.domain.port.UserPort;
import dentalbackend.dto.CompleteAppointmentRequest;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.dto.AppointmentResponse;
import dentalbackend.dto.PatientRecordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dentalbackend.domain.UserEntity;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService implements AppointmentUseCase {
    private final AppointmentPort repo;
    private final UserPort userRepo;
    private final PatientRecordUseCase patientRecordUseCase;

    @Override
    public AppointmentResponse create(CreateAppointmentRequest req, Long receptionistId) {
        // Provide clearer error messages when referenced users are missing
        UserEntity customer = userRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + req.getCustomerId()));
        UserEntity dentist = userRepo.findById(req.getDentistId())
                .orElseThrow(() -> new IllegalArgumentException("Dentist not found with id: " + req.getDentistId()));
        UserEntity receptionist = userRepo.findById(receptionistId)
                .orElseThrow(() -> new IllegalArgumentException("Receptionist (current user) not found with id: " + receptionistId));

        Appointment appt = Appointment.builder()
                .customer(customer)
                .dentist(dentist)
                .receptionist(receptionist)
                .scheduledTime(req.getScheduledTime())
                .notes(req.getNotes())
                .status(AppointmentStatus.PENDING)
                .build();
        Appointment saved = repo.save(appt);

        // If the customer is a regular CUSTOMER, promote them to PATIENT on first appointment
        try {
            if (customer.getRole() != null && customer.getRole() == dentalbackend.domain.UserRole.CUSTOMER) {
                customer.setRole(dentalbackend.domain.UserRole.PATIENT);
                userRepo.save(customer);
                log.info("Promoted user id={} username={} from CUSTOMER to PATIENT", customer.getId(), customer.getUsername());
            }
        } catch (Exception ex) {
            // don't fail appointment creation if role promotion fails; just log
            log.warn("Failed to promote customer to PATIENT for user id={} : {}", customer.getId(), ex.getMessage());
        }
        // map to DTO to avoid lazy-loading issues
        return mapToDto(saved);
    }

    // Return DTO list for dentist (use fetch)
    @Override
    public List<AppointmentResponse> dentistAppointments(Long dentistId) {
        return repo.findByDentistIdFetch(dentistId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Return DTO list for customer (use fetch)
    @Override
    public List<AppointmentResponse> customerAppointments(Long customerId) {
        return repo.findByCustomerIdFetch(customerId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Updated: return DTO to avoid lazy-loading issues
    @Override
    public AppointmentResponse getAppointmentForUser(Long id, UserEntity requester) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        boolean allowed = false;
        switch (requester.getRole()) {
            case ADMIN:
                allowed = true; break;
            case RECEPTIONIST:
                allowed = true; break;
            case DENTIST:
                allowed = appt.getDentist() != null && appt.getDentist().getId().equals(requester.getId()); break;
            case CUSTOMER:
            case PATIENT:
                allowed = appt.getCustomer() != null && appt.getCustomer().getId().equals(requester.getId()); break;
            default:
                allowed = false; break;
        }

        if (!allowed) throw new IllegalArgumentException("Access denied");

        return mapToDto(appt);
    }

    @Override
    public void confirmAppointment(Long id, Long actorUserId) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        UserEntity actor = userRepo.findById(actorUserId).orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorUserId));
        // Only receptionist or admin can confirm
        if (actor.getRole() != dentalbackend.domain.UserRole.RECEPTIONIST && actor.getRole() != dentalbackend.domain.UserRole.ADMIN) {
            throw new IllegalArgumentException("Only receptionist or admin can confirm appointments");
        }
        appt.setStatus(AppointmentStatus.CONFIRMED);
        repo.save(appt);
        log.info("Appointment id={} confirmed by userId={}", id, actorUserId);
    }

    @Override
    public void completeAppointment(Long id, Long actorUserId, CompleteAppointmentRequest req) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        UserEntity actor = userRepo.findById(actorUserId).orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorUserId));
        // Only dentist assigned to appt or admin can mark complete
        boolean isDentist = actor.getRole() == dentalbackend.domain.UserRole.DENTIST && appt.getDentist() != null && appt.getDentist().getId().equals(actor.getId());
        if (!isDentist && actor.getRole() != dentalbackend.domain.UserRole.ADMIN) {
            throw new IllegalArgumentException("Only assigned dentist or admin can complete appointments");
        }
        appt.setStatus(AppointmentStatus.COMPLETED);
        repo.save(appt);
        log.info("Appointment id={} marked COMPLETED by userId={}", id, actorUserId);

        // Create a patient record if provided or always create an entry
        try {
            PatientRecordRequest recordReq = new PatientRecordRequest();
            recordReq.setDentistId(appt.getDentist() != null ? appt.getDentist().getId() : null);
            recordReq.setDiagnosis(req != null ? req.getDiagnosis() : null);
            recordReq.setTreatmentPlan(req != null ? req.getTreatmentPlan() : null);
            // call patientRecordUseCase.create(patientId, recordReq)
            patientRecordUseCase.create(appt.getCustomer().getId(), recordReq);
        } catch (Exception ex) {
            log.warn("Failed to create patient record for appointment id={} : {}", appt.getId(), ex.getMessage());
        }
    }

    private AppointmentResponse mapToDto(Appointment appt) {
        Long customerId = appt.getCustomer() != null ? appt.getCustomer().getId() : null;
        String customerUsername = appt.getCustomer() != null ? appt.getCustomer().getUsername() : null;
        Long dentistId = appt.getDentist() != null ? appt.getDentist().getId() : null;
        String dentistUsername = appt.getDentist() != null ? appt.getDentist().getUsername() : null;
        Long receptionistId = appt.getReceptionist() != null ? appt.getReceptionist().getId() : null;
        String receptionistUsername = appt.getReceptionist() != null ? appt.getReceptionist().getUsername() : null;

        return AppointmentResponse.builder()
                .id(appt.getId())
                .status(appt.getStatus())
                .scheduledTime(appt.getScheduledTime())
                .notes(appt.getNotes())
                .createdAt(appt.getCreatedAt())
                .updatedAt(appt.getUpdatedAt())
                .customerId(customerId)
                .customerUsername(customerUsername)
                .dentistId(dentistId)
                .dentistUsername(dentistUsername)
                .receptionistId(receptionistId)
                .receptionistUsername(receptionistUsername)
                .build();
    }

    @Override
    public AppointmentResponse updateAppointment(Long id, Appointment updateReq, Long userId) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        if (!appt.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own appointments.");
        }
        // Update allowed fields
        appt.setScheduledTime(updateReq.getScheduledTime());
        appt.setNotes(updateReq.getNotes());
        appt.setStatus(updateReq.getStatus());
        Appointment saved = repo.save(appt);
        return mapToDto(saved);
    }

    @Override
    public void cancelAppointment(Long id, Long userId) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        if (!appt.getCustomer().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own appointments.");
        }
        appt.setStatus(AppointmentStatus.CANCELLED);
        repo.save(appt);
    }

    @Override
    public List<AppointmentResponse> allAppointments() {
        return repo.findAllWithRelations().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse adminUpdateAppointment(Long id, Appointment updateReq) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        appt.setScheduledTime(updateReq.getScheduledTime());
        appt.setNotes(updateReq.getNotes());
        appt.setStatus(updateReq.getStatus());
        Appointment saved = repo.save(appt);
        return mapToDto(saved);
    }

    @Override
    public void adminDeleteAppointment(Long id) {
        repo.deleteById(id);
    }
}

