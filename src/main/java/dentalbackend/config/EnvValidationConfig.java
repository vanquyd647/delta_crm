package dentalbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EnvValidationConfig implements ApplicationRunner {
    private final Environment env;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> missing = new ArrayList<>();

        // Required essentials
        check("DB_URL", missing);
        check("DB_USERNAME", missing);
        check("DB_PASSWORD", missing);
        check("REDIS_HOST", missing);
        check("REDIS_PORT", missing);
        check("APP_BASE_URL", missing);
        check("FRONTEND_URL", missing);
        check("JWT_SECRET", missing);
        check("JWT_ACCESS_TTL_MS", missing);
        check("JWT_REFRESH_TTL_MS", missing);

        // Mail (recommended if email features used)
        check("MAIL_HOST", missing);
        check("MAIL_PORT", missing);
        check("MAIL_USERNAME", missing);
        check("MAIL_PASSWORD", missing);

        // OAuth2 (recommended for Google login)
        check("GOOGLE_CLIENT_ID", missing);
        check("GOOGLE_CLIENT_SECRET", missing);

        // App behavior flags
        check("APP_AUTH_SET_COOKIES", missing);

        if (!missing.isEmpty()) {
            String msg = "Missing required environment variables: " + String.join(", ", missing) +
                    ".\nPlease copy .env.example to .env and fill values, or set these in your environment.";
            throw new IllegalStateException(msg);
        }

        // Additional sanity checks
        String jwtSecret = env.getProperty("JWT_SECRET");
        if (jwtSecret != null && jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters for adequate security");
        }

        // Numeric checks (read into locals to avoid static-analysis null warnings)
        String accessTtl = env.getProperty("JWT_ACCESS_TTL_MS");
        String refreshTtl = env.getProperty("JWT_REFRESH_TTL_MS");
        try {
            if (accessTtl == null || refreshTtl == null) {
                throw new IllegalStateException("JWT_ACCESS_TTL_MS and JWT_REFRESH_TTL_MS must be set");
            }
            Long.parseLong(accessTtl);
            Long.parseLong(refreshTtl);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("JWT_ACCESS_TTL_MS and JWT_REFRESH_TTL_MS must be valid integers (milliseconds)");
        }

        String redisPort = env.getProperty("REDIS_PORT");
        String mailPort = env.getProperty("MAIL_PORT");
        try {
            if (redisPort == null || mailPort == null) {
                throw new IllegalStateException("REDIS_PORT and MAIL_PORT must be set");
            }
            Integer.parseInt(redisPort);
            Integer.parseInt(mailPort);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("REDIS_PORT and MAIL_PORT must be valid integers");
        }

        // If we reach here, env is validated
    }

    private void check(String key, List<String> missing) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) missing.add(key);
    }
}
