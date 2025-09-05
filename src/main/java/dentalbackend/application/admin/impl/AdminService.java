package dentalbackend.application.admin.impl;

import dentalbackend.application.admin.AdminUseCase;
import dentalbackend.domain.port.PaymentPort;
import dentalbackend.domain.port.AppointmentPort;
import dentalbackend.domain.port.ServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {
    private final PaymentPort paymentPort;
    private final AppointmentPort appointmentPort;
    private final ServicePort servicePort;

    @Override
    public Double getTotalRevenue() {
        return paymentPort.findAll().stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
    }

    @Override
    public Map<LocalDate, Double> getRevenueByDay() {
        return paymentPort.findAll().stream()
                .collect(Collectors.groupingBy(
                        p -> LocalDate.ofInstant(p.getPaidAt(), ZoneId.systemDefault()),
                        Collectors.summingDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                ));
    }

    @Override
    public Map<String, Long> getServiceUsage() {
        List<dentalbackend.domain.Appointment> appointments = appointmentPort.findAllWithRelations();
        Map<String, Long> usage = new HashMap<>();
        for (dentalbackend.domain.Service s : servicePort.findAll()) {
            long count = appointments.stream()
                    .filter(a -> a.getNotes() != null && a.getNotes().contains(s.getName()))
                    .count();
            usage.put(s.getName(), count);
        }
        return usage;
    }
}

