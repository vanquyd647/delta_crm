package dentalbackend.application.payment;

import dentalbackend.domain.Payment;

import java.util.List;

public interface PaymentUseCase {
    Payment recordPayment(Long appointmentId, Double amount, String method, String invoiceNumber);
    List<Payment> getPaymentsForAppointment(Long appointmentId);
    Payment getPayment(Long paymentId);
}

