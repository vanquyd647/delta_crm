package dentalbackend.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

@Component
public class GoogleRecaptchaVerifier implements CaptchaVerifier {
    @Value("${RECAPTCHA_SECRET}")
    private String recaptchaSecret;

    @Value("${RECAPTCHA_MIN_SCORE:0.5}")
    private double minScore;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean verify(String token) {
        String url = "https://www.google.com/recaptcha/api/siteverify";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "secret=" + recaptchaSecret + "&response=" + token;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> result = response.getBody();
            if (result == null) return false;
            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (!success) return false;
            if (result.containsKey("score")) {
                double score = ((Number) result.get("score")).doubleValue();
                return score >= minScore;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

