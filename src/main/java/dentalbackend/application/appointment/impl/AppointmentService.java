package dentalbackend.application.appointment.impl;

import dentalbackend.application.appointment.AppointmentUseCase;
import dentalbackend.application.patient.PatientRecordUseCase;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.AppointmentStatus;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.domain.port.UserPort;
import dentalbackend.domain.port.ServicePort;
import java.util.List;
import java.util.stream.Collectors;
import dentalbackend.dto.CompleteAppointmentRequest;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.dto.AppointmentResponse;
import dentalbackend.dto.PatientRecordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dentalbackend.domain.UserEntity;

// new import for profile repo
import dentalbackend.repository.UserProfileRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService implements AppointmentUseCase {
    private final AppointmentPort repo;
    private final UserPort userRepo;
    private final PatientRecordUseCase patientRecordUseCase;
    private final ServicePort servicePort;
    private final UserProfileRepository profileRepo; // injected to read emergency contact

    @Override
    public AppointmentResponse create(CreateAppointmentRequest req, Long receptionistId) {
        // Provide clearer error messages when referenced users are missing
        UserEntity customer = userRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + req.getCustomerId()));
        UserEntity dentist = userRepo.findById(req.getDentistId())
                .orElseThrow(() -> new IllegalArgumentException("Dentist not found with id: " + req.getDentistId()));
        UserEntity receptionist = userRepo.findById(receptionistId)
                .orElseThrow(() -> new IllegalArgumentException("Receptionist (current user) not found with id: " + receptionistId));

        // Resolve service
        dentalbackend.domain.Service service = servicePort.findById(req.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + req.getServiceId()));

        Appointment appt = Appointment.builder()
                .customer(customer)
                .dentist(dentist)
                .receptionist(receptionist)
                .service(service)
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

        boolean allowed = switch (requester.getRole()) {
            case ADMIN, RECEPTIONIST -> true;
            case DENTIST -> appt.getDentist() != null && appt.getDentist().getId().equals(requester.getId());
            case CUSTOMER, PATIENT -> appt.getCustomer() != null && appt.getCustomer().getId().equals(requester.getId());
            default -> false;
        };

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
        String customerEmail = appt.getCustomer() != null ? appt.getCustomer().getEmail() : null;
        String customerEmergency = null;
        try {
            if (appt.getCustomer() != null) {
                customerEmergency = profileRepo.findByUser(appt.getCustomer()).map(p -> p.getEmergencyContact()).orElse(null);
            }
        } catch (Exception ex) {
            log.debug("Failed to load user profile for customer id={}: {}", customerId, ex.getMessage());
        }

        Long dentistId = appt.getDentist() != null ? appt.getDentist().getId() : null;
        String dentistUsername = appt.getDentist() != null ? appt.getDentist().getUsername() : null;
        Long receptionistId = appt.getReceptionist() != null ? appt.getReceptionist().getId() : null;
        String receptionistUsername = appt.getReceptionist() != null ? appt.getReceptionist().getUsername() : null;
        Long serviceId = appt.getService() != null ? appt.getService().getId() : null;
        String serviceName = appt.getService() != null ? appt.getService().getName() : null;

        return AppointmentResponse.builder()
                .id(appt.getId())
                .status(appt.getStatus())
                .scheduledTime(appt.getScheduledTime())
                .notes(appt.getNotes())
                .createdAt(appt.getCreatedAt())
                .updatedAt(appt.getUpdatedAt())
                .customerId(customerId)
                .customerUsername(appt.getCustomer() != null && appt.getCustomer().getFullName() != null && !appt.getCustomer().getFullName().isBlank()
                        ? appt.getCustomer().getFullName() : customerUsername)
                .customerEmail(customerEmail)
                .customerEmergencyContact(customerEmergency)
                .dentistId(dentistId)
                .dentistUsername(appt.getDentist() != null && appt.getDentist().getFullName() != null && !appt.getDentist().getFullName().isBlank()
                        ? appt.getDentist().getFullName() : dentistUsername)
                .receptionistId(receptionistId)
                .receptionistUsername(appt.getReceptionist() != null && appt.getReceptionist().getFullName() != null && !appt.getReceptionist().getFullName().isBlank()
                        ? appt.getReceptionist().getFullName() : receptionistUsername)
                .serviceId(serviceId)
                .serviceName(serviceName)
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
        // Do not allow customers to change service/dentist via this endpoint to avoid conflicts
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
        // allow admin to change service/dentist when provided
        if (updateReq.getService() != null) appt.setService(updateReq.getService());
        if (updateReq.getDentist() != null) appt.setDentist(updateReq.getDentist());
        Appointment saved = repo.save(appt);
        return mapToDto(saved);
    }

    @Override
    public void adminDeleteAppointment(Long id) {
        repo.deleteById(id);
    }
}
