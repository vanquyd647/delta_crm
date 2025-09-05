package dentalbackend.captcha;

import org.springframework.stereotype.Component;

@Component
public class DummyCaptchaVerifier implements CaptchaVerifier {
    @Override
    public boolean verify(String token) {
        // TODO: integrate with Google reCAPTCHA or hCaptcha
        return true;
    }
}
