package com.yuno.assignment.idempotency;

import com.yuno.assignment.properties.IdempotencyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.*;

@Slf4j
@Component
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "pos:idem:";
    private static final String IN_FLIGHT_MARKER = "__IN_FLIGHT__";

    private static int retryCount = 0;

    private final StringRedisTemplate redis;
    private final IdempotencyProperties properties;

    public RedisIdempotencyStore(StringRedisTemplate redis, IdempotencyProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public IdempotencyResult store(String key) {
        if(retryCount >= 3){
            return IdempotencyResult.getResult(FAILED, "Failed to set idempotency cache");
        }
        String redisKey = getKey(key);
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(redisKey, IN_FLIGHT_MARKER, properties.getInFlightTtl());
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Idempotency key {} reserved as in-flight", key);
            return IdempotencyResult.getResult(RESERVED);
        }
        String existing = redis.opsForValue().get(redisKey);
        if (existing == null) {
            //Retry the reservation, max 3 times.
            retryCount++;
            return store(key);
        }
        if (IN_FLIGHT_MARKER.equals(existing)) {
            return IdempotencyResult.getResult(IN_FLIGHT);
        }
        return IdempotencyResult.getResult(COMPLETED, existing);
    }

    @Override
    public void commit(String key, String responseJson) {
        redis.opsForValue().set(getKey(key), responseJson, properties.getTtl());
    }

    @Override
    public void release(String key) {
        redis.delete(getKey(key));
    }

    private static String getKey(String key) {
        return KEY_PREFIX + key;
    }
}
