package dentalbackend.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redis;

    /** Simple fixed-window rate-limit: allow up to `limit` per `window`. */
    public boolean isAllowed(String key, int limit, Duration window) {
        String redisKey = "rl:" + key + ":" + (System.currentTimeMillis() / window.toMillis());
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, window);
        }
        return count != null && count <= limit;
    }
}
