package dentalbackend.application.appointment.impl;

import dentalbackend.application.appointment.AppointmentUseCase;
import dentalbackend.application.patient.PatientRecordUseCase;
import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.AppointmentStatus;
import dentalbackend.domain.UserProfile;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.domain.port.UserPort;
import dentalbackend.domain.port.ServicePort;
import dentalbackend.dto.CompleteAppointmentRequest;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.dto.AppointmentResponse;
import dentalbackend.dto.PatientRecordRequest;
import dentalbackend.dto.DentistResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import dentalbackend.domain.UserEntity;
import dentalbackend.repository.UserProfileRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService implements AppointmentUseCase {
    private final AppointmentPort repo;
    private final UserPort userRepo;
    private final PatientRecordUseCase patientRecordUseCase;
    private final ServicePort servicePort;
    private final UserProfileRepository profileRepo;

    private final DentistUseCase dentistUseCase;

    // Check whether a given userId is the assigned dentist for the appointment.
    private boolean isAssignedDentist(Appointment appt, Long userId) {
        if (appt == null || userId == null) return false;
        try {
            if (appt.getDentist() != null && appt.getDentist().getId() != null && appt.getDentist().getId().equals(userId)) {
                return true;
            }
            if (appt.getDentistRefId() != null) {
                try {
                    DentistResponse d = dentistUseCase.getById(appt.getDentistRefId());
                    return d != null && d.getUserId() != null && d.getUserId().equals(userId);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            log.debug("Error resolving assigned dentist for appt id={}: {}", appt == null ? null : appt.getId(), ex.getMessage());
        }
        return false;
    }

    @Override
    public AppointmentResponse create(CreateAppointmentRequest req, Long receptionistId) {
        UserEntity customer = userRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + req.getCustomerId()));
        UserEntity dentist = userRepo.findById(req.getDentistId())
                .orElseThrow(() -> new IllegalArgumentException("Dentist not found with id: " + req.getDentistId()));
        UserEntity receptionist = userRepo.findById(receptionistId)
                .orElseThrow(() -> new IllegalArgumentException("Receptionist not found with id: " + receptionistId));

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

        try {
            if (customer.getRole() != null && customer.getRole() == dentalbackend.domain.UserRole.CUSTOMER) {
                customer.setRole(dentalbackend.domain.UserRole.PATIENT);
                try {
                    userRepo.save(customer);
                    log.info("Promoted user id={} username={} from CUSTOMER to PATIENT", customer.getId(), customer.getUsername());
                } catch (Exception e) {
                    log.warn("Failed to persist promoted role for user id={}: {}", customer.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during customer role promotion: {}", e.getMessage(), e);
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> dentistAppointments(Long dentistId) {
        return repo.findByDentistIdFetch(dentistId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> customerAppointments(Long customerId) {
        return repo.findByCustomerIdFetch(customerId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentForUser(Long id, Long requesterId) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        UserEntity requester = userRepo.findById(requesterId).orElseThrow(() -> new IllegalArgumentException("Requester not found: " + requesterId));

        boolean allowed = false;
        if (requester.getRole() == dentalbackend.domain.UserRole.ADMIN || requester.getRole() == dentalbackend.domain.UserRole.RECEPTIONIST) {
            allowed = true;
        } else if (requester.getRole() == dentalbackend.domain.UserRole.DENTIST) {
            allowed = isAssignedDentist(appt, requester.getId());
        } else if (requester.getRole() == dentalbackend.domain.UserRole.CUSTOMER || requester.getRole() == dentalbackend.domain.UserRole.PATIENT) {
            allowed = appt.getCustomer() != null && appt.getCustomer().getId().equals(requester.getId());
        }

        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        return mapToDto(appt);
    }

    @Override
    @Transactional
    public void confirmAppointment(Long id, Long actorUserId) {
        UserEntity actor = userRepo.findById(actorUserId).orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorUserId));
        if (actor.getRole() != dentalbackend.domain.UserRole.RECEPTIONIST && actor.getRole() != dentalbackend.domain.UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only receptionist or admin can confirm appointments");
        }
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
        appt.setStatus(AppointmentStatus.CONFIRMED);
        repo.save(appt);
        log.info("Appointment id={} confirmed by userId={}", id, actorUserId);
    }

    @Override
    @Transactional
    public void completeAppointment(Long id, Long actorUserId, CompleteAppointmentRequest req) {
        UserEntity actor = userRepo.findById(actorUserId).orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorUserId));
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        boolean isDentist = actor.getRole() == dentalbackend.domain.UserRole.DENTIST && isAssignedDentist(appt, actor.getId());
        if (!isDentist && actor.getRole() != dentalbackend.domain.UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only assigned dentist or admin can complete appointments");
        }

        appt.setStatus(AppointmentStatus.COMPLETED);
        repo.save(appt);

        if (appt.getCustomer() != null) {
            try {
                PatientRecordRequest recordReq = new PatientRecordRequest();
                recordReq.setDentistId(appt.getDentist() != null ? appt.getDentist().getId() : null);
                recordReq.setTreatmentPlan(req != null ? req.getTreatmentPlan() : null);
                patientRecordUseCase.create(appt.getCustomer().getId(), recordReq);
            } catch (Exception ex) {
                log.warn("Failed to create patient record for appointment id={} : {}", appt.getId(), ex.getMessage());
            }
        }
    }

    private AppointmentResponse mapToDto(Appointment appt) {
        if (appt == null) return null;

        Long customerId = appt.getCustomer() != null ? appt.getCustomer().getId() : null;
        String customerUsername = appt.getCustomer() != null ? appt.getCustomer().getUsername() : null;
        String customerEmail = appt.getCustomer() != null ? appt.getCustomer().getEmail() : null;
        String customerEmergency = null;
        try {
            if (appt.getCustomer() != null) {
                customerEmergency = profileRepo.findByUser(appt.getCustomer()).map(UserProfile::getEmergencyContact).orElse(null);
            }
        } catch (Exception ex) {
            log.debug("Failed to load user profile for customer id={}: {}", customerId, ex.getMessage());
        }

        Long dentistId = appt.getDentist() != null ? appt.getDentist().getId() : appt.getDentistRefId();
        String dentistUsername = null;
        try {
            if (appt.getDentist() != null) {
                dentistUsername = appt.getDentist().getFullName() != null && !appt.getDentist().getFullName().isBlank()
                        ? appt.getDentist().getFullName() : appt.getDentist().getUsername();
            } else if (appt.getDentistRefId() != null) {
                DentistResponse d = dentistUseCase.getById(appt.getDentistRefId());
                if (d != null) dentistUsername = d.getName();
            }
        } catch (Exception ignored) {
        }

        Long receptionistId = appt.getReceptionist() != null ? appt.getReceptionist().getId() : null;
        String receptionistUsername = appt.getReceptionist() != null ? (appt.getReceptionist().getFullName() != null && !appt.getReceptionist().getFullName().isBlank() ? appt.getReceptionist().getFullName() : appt.getReceptionist().getUsername()) : null;

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
                .customerUsername(customerUsername)
                .customerEmail(customerEmail)
                .customerEmergencyContact(customerEmergency)
                .dentistId(dentistId)
                .dentistUsername(dentistUsername)
                .receptionistId(receptionistId)
                .receptionistUsername(receptionistUsername)
                .serviceId(serviceId)
                .serviceName(serviceName)
                .build();
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointment(Long id, Appointment updateReq, Long userId) {
        UserEntity actor = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        boolean isAdmin = actor.getRole() == dentalbackend.domain.UserRole.ADMIN;
        boolean isOwner = appt.getCustomer() != null && appt.getCustomer().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the customer or admin can update this appointment");
        }

        if (updateReq.getScheduledTime() != null) appt.setScheduledTime(updateReq.getScheduledTime());
        if (updateReq.getNotes() != null) appt.setNotes(updateReq.getNotes());

        if (isAdmin) {
            if (updateReq.getService() != null) appt.setService(updateReq.getService());
            if (updateReq.getDentist() != null) appt.setDentist(updateReq.getDentist());
        }

        Appointment saved = repo.save(appt);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id, Long userId) {
        UserEntity actor = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        boolean isOwnerCancel = appt.getCustomer() != null && appt.getCustomer().getId().equals(userId);
        boolean isAdminCancel = actor.getRole() == dentalbackend.domain.UserRole.ADMIN;

        if (!isOwnerCancel && !isAdminCancel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the customer or admin can cancel this appointment");
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        repo.save(appt);
        log.info("Appointment id={} cancelled by userId={}", id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> allAppointments() {
        return repo.findAllWithRelations().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppointmentResponse adminUpdateAppointment(Long id, Appointment updateReq) {
        Appointment appt = repo.findByIdFetch(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));

        if (updateReq.getScheduledTime() != null) appt.setScheduledTime(updateReq.getScheduledTime());
        if (updateReq.getNotes() != null) appt.setNotes(updateReq.getNotes());
        if (updateReq.getService() != null) appt.setService(updateReq.getService());
        if (updateReq.getDentist() != null) appt.setDentist(updateReq.getDentist());

        Appointment saved = repo.save(appt);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void adminDeleteAppointment(Long id) {
        repo.deleteById(id);
    }
}

