package com.example.prescription.cache;

import com.example.prescription.dto.PrescriptionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Simple cache-aside layer for prescriptions backed by Redis.
 * Fails open (logs and bypasses cache) to avoid cascading outages when Redis is unhealthy.
 */
@Component
public class PrescriptionCacheService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionCacheService.class);

    private static final String KEY_PREFIX = "prescription:";

    // Reasonable freshness window; tuned based on product/SRE needs.
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, PrescriptionDto> redisTemplate;

    public PrescriptionCacheService(RedisTemplate<String, PrescriptionDto> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<PrescriptionDto> get(Long id) {
        String key = buildKey(id);
        try {
            PrescriptionDto dto = redisTemplate.opsForValue().get(key);
            if (dto != null) {
                log.debug("Redis cache hit for key={}", key);
            } else {
                log.debug("Redis cache miss for key={}", key);
            }
            return Optional.ofNullable(dto);
        } catch (Exception ex) {
            log.warn("Failed to read prescription from Redis for key={}. Bypassing cache.", key, ex);
            return Optional.empty();
        }
    }

    public void put(PrescriptionDto dto) {
        put(dto, DEFAULT_TTL);
    }

    public void put(PrescriptionDto dto, Duration ttl) {
        String key = buildKey(dto.getId());
        try {
            redisTemplate.opsForValue().set(key, dto, ttl);
            log.debug("Cached prescription {} with TTL {}s", dto.getId(), ttl.getSeconds());
        } catch (Exception ex) {
            log.warn("Failed to write prescription {} to Redis. Continuing without cache.", dto.getId(), ex);
        }
    }

    public void evict(Long id) {
        String key = buildKey(id);
        try {
            redisTemplate.delete(key);
            log.debug("Evicted prescription {} from cache", id);
        } catch (Exception ex) {
            log.warn("Failed to evict prescription {} from Redis. Continuing.", id, ex);
        }
    }

    private String buildKey(Long id) {
        return KEY_PREFIX + id;
    }
}
