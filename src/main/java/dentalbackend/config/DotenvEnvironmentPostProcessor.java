package dentalbackend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DOTENV = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path p = Path.of(DOTENV);
        if (!Files.exists(p)) return;

        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String raw : Files.readAllLines(p)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // remove optional surrounding quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) map.put(key, value);
            }
            if (!map.isEmpty()) {
                MutablePropertySources sources = environment.getPropertySources();
                MapPropertySource mps = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
                // put with highest precedence
                sources.addFirst(mps);
            }
        } catch (IOException e) {
            // ignore, do not fail startup here; validation will catch missing vars
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

