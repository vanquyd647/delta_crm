package dentalbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
// Exclude the old flat "service" package entirely to avoid duplicate @Service beans
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "dentalbackend\\.service\\..*") )
public class DentalBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentalBackendApplication.class, args);
    }

}
