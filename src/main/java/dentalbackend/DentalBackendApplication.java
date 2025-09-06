package dentalbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
// Exclude the old flat "service" package and legacy_service_backup to avoid duplicate @Service beans
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dentalbackend\\.service\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dentalbackend\\.legacy_service_backup\\..*")
})
public class DentalBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalBackendApplication.class, args);
    }

}
