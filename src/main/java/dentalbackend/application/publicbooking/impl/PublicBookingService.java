package dentalbackend.application.publicbooking.impl;

import dentalbackend.application.publicbooking.PublicBookingUseCase;
import dentalbackend.dto.ConsultationRequest;
import dentalbackend.dto.QuickBookingRequest;
import dentalbackend.dto.AppointmentResponse;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.AppointmentStatus;
import dentalbackend.domain.UserEntity;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.domain.port.UserPort;
import dentalbackend.domain.port.ServicePort;
import dentalbackend.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicBookingService implements PublicBookingUseCase {
    private final UserPort userPort;
    private final AppointmentPort appointmentPort;
    private final ServicePort servicePort;
    private final EmailService emailService;

    @Value("${clinic.contact.email:homequy001@gmail.com}")
    private String clinicEmail;

    @Override
    public void submitConsultation(ConsultationRequest req) {
        String subject = String.format("New consultation request from %s", req.getFullName());
        StringBuilder body = new StringBuilder();
        body.append("Full name: ").append(req.getFullName()).append("\n");
        body.append("Email: ").append(req.getEmail()).append("\n");
        body.append("Phone: ").append(req.getPhone()).append("\n");
        body.append("Preferred method: ").append(req.getMethod()).append("\n\n");
        body.append("Message:\n").append(req.getContent() == null ? "" : req.getContent());

        try {
            emailService.send(clinicEmail, subject, body.toString());
            log.info("Consultation request sent to {} from {}", clinicEmail, req.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send consultation email: {}", ex.getMessage());
        }
    }

    @Override
    public AppointmentResponse quickBook(QuickBookingRequest req) {
        // Parse date (support multiple common formats) and time
        LocalDate date = null;
        String dateStr = req.getDate() == null ? "" : req.getDate().trim();
        DateTimeFormatter[] dateFormatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ISO_LOCAL_DATE
        };
        for (DateTimeFormatter fmt : dateFormatters) {
            try {
                date = LocalDate.parse(dateStr, fmt);
                break;
            } catch (DateTimeParseException ignored) {
            }
        }
        if (date == null) {
            throw new IllegalArgumentException("Unable to parse date '" + req.getDate() + "'. Expected formats: MM/dd/yyyy or dd/MM/yyyy or yyyy-MM-dd");
        }

        LocalTime time;
        try {
            time = LocalTime.parse(req.getTime(), DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Unable to parse time '" + req.getTime() + "'. Expected format: HH:mm (24-hour), e.g. 14:30");
        }

        ZoneId zone = ZoneId.systemDefault();
        Instant scheduledInstant = ZonedDateTime.of(date, time, zone).toInstant();

        // Find or create customer by email
        Optional<UserEntity> existing = userPort.findByEmail(req.getEmail());
        UserEntity customer = existing.orElseGet(() -> {
            String username = generateUsernameFromEmail(req.getEmail());
            UserEntity u = UserEntity.builder()
                    .username(username)
                    .email(req.getEmail())
                    .passwordHash(UUID.randomUUID().toString())
                    .role(dentalbackend.domain.UserRole.CUSTOMER)
                    .provider(dentalbackend.domain.AuthProvider.LOCAL)
                    .fullName(req.getFullName())
                    .emailVerified(false)
                    .enabled(true)
                    .build();
            try {
                return userPort.save(u);
            } catch (Exception ex) {
                log.warn("Failed to create customer user for quick booking: {}", ex.getMessage());
                return u;
            }
        });

        // Find optional dentist
        UserEntity dentist = null;
        if (req.getDentistId() != null) {
            dentist = userPort.findById(req.getDentistId()).orElse(null);
        }

        // Resolve service
        dentalbackend.domain.Service service = servicePort.findById(req.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + req.getServiceId()));

        // Build and save appointment
        Appointment appt = Appointment.builder()
                .customer(customer)
                .dentist(dentist)
                .receptionist(null)
                .service(service)
                .scheduledTime(scheduledInstant)
                .notes((req.getNotes() == null ? "" : req.getNotes()) + "\nService: " + service.getName())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentPort.save(appt);

        // Send confirmation email to user
        try {
            String subjUser = "Your booking request has been received";
            StringBuilder bodyUser = new StringBuilder();
            bodyUser.append("Hello ").append(req.getFullName()).append(",\n\n");
            bodyUser.append("We received your booking request for service: ").append(service.getName()).append("\n");
            if (dentist != null) {
                String dentistDisplay = dentist.getFullName() != null && !dentist.getFullName().isBlank()
                        ? dentist.getFullName()
                        : dentist.getUsername();
                bodyUser.append("Preferred dentist: ").append(dentistDisplay).append("\n");
            }
            bodyUser.append("Scheduled for: ").append(req.getDate()).append(" ").append(req.getTime()).append("\n\n");
            bodyUser.append("We will contact you shortly to confirm the appointment.\n\n");
            bodyUser.append("Clinic contact: ").append(clinicEmail);

            emailService.send(req.getEmail(), subjUser, bodyUser.toString());
        } catch (Exception ex) {
            log.warn("Failed to send booking confirmation to user {} : {}", req.getEmail(), ex.getMessage());
        }

        // Send notification email to clinic
        try {
            String subjClinic = "New quick booking received";
            StringBuilder bodyClinic = new StringBuilder();
            bodyClinic.append("Customer: ").append(req.getFullName()).append(" (").append(req.getEmail()).append(")\n");
            bodyClinic.append("Phone: ").append(req.getPhone()).append("\n");
            bodyClinic.append("Service: ").append(service.getName()).append("\n");
            bodyClinic.append("Scheduled: ").append(req.getDate()).append(" ").append(req.getTime()).append("\n");
            if (dentist != null) {
                String dentistDisplay = dentist.getFullName() != null && !dentist.getFullName().isBlank()
                        ? dentist.getFullName()
                        : dentist.getUsername();
                bodyClinic.append("Preferred dentist: ").append(dentistDisplay).append("\n");
            }
            bodyClinic.append("Notes: \n").append(req.getNotes() == null ? "" : req.getNotes());

            emailService.send(clinicEmail, subjClinic, bodyClinic.toString());
        } catch (Exception ex) {
            log.warn("Failed to send booking notification to clinic: {}", ex.getMessage());
        }

        // map to AppointmentResponse
        AppointmentResponse resp = AppointmentResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .scheduledTime(saved.getScheduledTime())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .customerId(saved.getCustomer() != null ? saved.getCustomer().getId() : null)
                .customerUsername(saved.getCustomer() != null ? saved.getCustomer().getUsername() : null)
                .dentistId(saved.getDentist() != null ? saved.getDentist().getId() : null)
                .dentistUsername(saved.getDentist() != null ? saved.getDentist().getUsername() : null)
                .receptionistId(saved.getReceptionist() != null ? saved.getReceptionist().getId() : null)
                .receptionistUsername(saved.getReceptionist() != null ? saved.getReceptionist().getUsername() : null)
                .serviceId(saved.getService() != null ? saved.getService().getId() : null)
                .serviceName(saved.getService() != null ? saved.getService().getName() : null)
                .build();

        return resp;
    }

    private String generateUsernameFromEmail(String email) {
        String local = email.split("@")[0];
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String candidate = (local + "_" + suffix).replaceAll("[^A-Za-z0-9_\\.-]", "");
        if (candidate.length() > 64) candidate = candidate.substring(0, 64);
        int attempt = 0;
        String finalUsername = candidate;
        while (userPort.existsByUsername(finalUsername)) {
            attempt++;
            finalUsername = candidate + attempt;
            if (attempt > 10) break;
        }
        return finalUsername;
    }
}
