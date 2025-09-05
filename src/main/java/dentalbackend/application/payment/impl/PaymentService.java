package dentalbackend.application.payment.impl;

import dentalbackend.application.payment.PaymentUseCase;
import dentalbackend.domain.Payment;
import dentalbackend.domain.Appointment;
import dentalbackend.domain.port.PaymentPort;
import dentalbackend.domain.port.AppointmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("applicationPaymentService")
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {
    private final PaymentPort paymentPort;
    private final AppointmentPort appointmentPort;

    @Override
    @Transactional
    public Payment recordPayment(Long appointmentId, Double amount, String method, String invoiceNumber) {
        Appointment appt = appointmentPort.findById(appointmentId).orElseThrow();
        Payment payment = Payment.builder()
                .appointment(appt)
                .amount(amount)
                .method(method)
                .invoiceNumber(invoiceNumber)
                .build();
        return paymentPort.save(payment);
    }

    @Override
    public List<Payment> getPaymentsForAppointment(Long appointmentId) {
        Appointment appt = appointmentPort.findById(appointmentId).orElseThrow();
        return paymentPort.findByAppointment(appt);
    }

    @Override
    public Payment getPayment(Long paymentId) {
        return paymentPort.findById(paymentId).orElse(null);
    }
}
