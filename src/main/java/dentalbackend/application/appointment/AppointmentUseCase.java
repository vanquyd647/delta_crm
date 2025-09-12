package dentalbackend.application.appointment;

import dentalbackend.dto.AppointmentResponse;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.dto.UpdateAppointmentRequest;
import dentalbackend.dto.CompleteAppointmentRequest;
import dentalbackend.domain.Appointment;

import java.util.List;

public interface AppointmentUseCase {
    AppointmentResponse create(CreateAppointmentRequest req, Long receptionistId);
    List<AppointmentResponse> dentistAppointments(Long dentistId);
    List<AppointmentResponse> customerAppointments(Long customerId);
    AppointmentResponse getAppointmentForUser(Long id, Long requesterId);
    AppointmentResponse updateAppointment(Long id, UpdateAppointmentRequest updateReq, Long userId);
    void cancelAppointment(Long id, Long userId);
    List<AppointmentResponse> allAppointments();
    AppointmentResponse adminUpdateAppointment(Long id, Appointment updateReq);
    void adminDeleteAppointment(Long id);

    // New role-specific transitions
    void confirmAppointment(Long id, Long actorUserId);
    void completeAppointment(Long id, Long actorUserId, CompleteAppointmentRequest req);
}
