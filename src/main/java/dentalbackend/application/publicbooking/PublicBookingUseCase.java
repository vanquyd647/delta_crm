package dentalbackend.application.publicbooking;

import dentalbackend.dto.ConsultationRequest;
import dentalbackend.dto.QuickBookingRequest;
import dentalbackend.dto.AppointmentResponse;

public interface PublicBookingUseCase {
    void submitConsultation(ConsultationRequest req);
    AppointmentResponse quickBook(QuickBookingRequest req);
}

