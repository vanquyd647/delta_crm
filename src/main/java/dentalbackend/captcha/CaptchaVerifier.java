package dentalbackend.captcha;

public interface CaptchaVerifier {
    boolean verify(String token);
}