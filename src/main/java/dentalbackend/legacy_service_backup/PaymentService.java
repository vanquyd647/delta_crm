package dentalbackend.service;

import dentalbackend.domain.Payment;
import dentalbackend.domain.Appointment;
import dentalbackend.repository.PaymentRepository;
import dentalbackend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import dentalbackend.application.payment.PaymentUseCase;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {
    private final PaymentRepository paymentRepo;
    private final AppointmentRepository appointmentRepo;

    @Override
    @Transactional
    public Payment recordPayment(Long appointmentId, Double amount, String method, String invoiceNumber) {
        Appointment appt = appointmentRepo.findById(appointmentId).orElseThrow();
        Payment payment = Payment.builder()
                .appointment(appt)
                .amount(amount)
                .method(method)
                .invoiceNumber(invoiceNumber)
                .build();
        return paymentRepo.save(payment);
    }

    @Override
    public List<Payment> getPaymentsForAppointment(Long appointmentId) {
        Appointment appt = appointmentRepo.findById(appointmentId).orElseThrow();
        return paymentRepo.findByAppointment(appt);
    }

    @Override
    public Payment getPayment(Long paymentId) {
        return paymentRepo.findById(paymentId).orElse(null);
    }
}
