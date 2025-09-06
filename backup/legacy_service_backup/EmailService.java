package dentalbackend.legacy_service_backup;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;

@RequiredArgsConstructor
public class EmailService {

    // Legacy backup - not a Spring bean. Use infrastructure.email.EmailService for real sending.

    @Value("${spring.mail.username:no-reply@example.com}")
    private String from;

    public void send(String to, String subject, String body) {
        // legacy placeholder: real implementation lives in infrastructure.email.EmailService
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        // no mailSender here - legacy placeholder
    }
}
