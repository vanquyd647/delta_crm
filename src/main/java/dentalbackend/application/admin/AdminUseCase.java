package dentalbackend.application.admin;

import java.time.LocalDate;
import java.util.Map;

public interface AdminUseCase {
    Double getTotalRevenue();
    Map<LocalDate, Double> getRevenueByDay();
    Map<String, Long> getServiceUsage();
}

