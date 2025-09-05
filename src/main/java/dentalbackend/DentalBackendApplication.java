package dentalbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dentalbackend\\.service\\.(ServiceService|PaymentService|PatientRecordService|AuthService|AppointmentService|AdminService|ServiceProfileService|UserService)") )
public class DentalBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalBackendApplication.class, args);
    }

}
