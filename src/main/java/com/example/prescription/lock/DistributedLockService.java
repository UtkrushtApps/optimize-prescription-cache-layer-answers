package com.example.prescription.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Simple Redis-based distributed lock using SET NX with expiration.
 *
 * Lock ownership is tracked via a unique value per acquirer so we only release
 * locks that we actually hold.
 */
@Component
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private static final String LOCK_PREFIX = "lock:prescription:";

    private final StringRedisTemplate stringRedisTemplate;

    public DistributedLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String buildLockKeyForPrescription(Long id) {
        return LOCK_PREFIX + id;
    }

    /**
     * Attempts to acquire a lock.
     *
     * @param key      lock key
     * @param value    unique lock owner value (e.g. UUID)
     * @param ttl      lock time-to-live
     * @return true if acquired, false otherwise
     */
    public boolean tryLock(String key, String value, Duration ttl) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, value, ttl);
            boolean acquired = Boolean.TRUE.equals(success);
            if (acquired) {
                log.debug("Acquired distributed lock for key={}", key);
            } else {
                log.debug("Could not acquire distributed lock for key={} (already held)", key);
            }
            return acquired;
        } catch (DataAccessException e) {
            // Fail open: do not block writes entirely if Redis is down, but log prominently.
            log.error("Redis failure while trying to acquire distributed lock for key={}. Proceeding without lock.",
                    key, e);
            return true; // allow single-instance progress even if coordination is temporarily unavailable
        }
    }

    /**
     * Releases a lock if the caller still owns it.
     */
    public void unlock(String key, String value) {
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(key);
            if (value.equals(currentValue)) {
                stringRedisTemplate.delete(key);
                log.debug("Released distributed lock for key={}", key);
            } else {
                log.debug("Not releasing lock for key={} as ownership changed.", key);
            }
        } catch (DataAccessException e) {
            log.error("Redis failure while trying to release distributed lock for key={}", key, e);
        }
    }
}
