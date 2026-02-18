package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.*;

@Aspect
public class TelescopeCacheAspect {

    private final TelescopeStorage storage;
    private final TelescopeUserProvider userProvider;

    public TelescopeCacheAspect(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        this.storage = storage;
        this.userProvider = userProvider;
    }

    @Around("@annotation(cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        if (!storage.isEnabled()) return joinPoint.proceed();

        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long durationNanos = System.nanoTime() - startNanos;
        long durationMs = durationNanos / 1_000_000;

        // A cache HIT typically returns in microseconds (< 1ms) since
        // the method body is never executed. MISS executes the method body.
        String operation = durationNanos < 500_000 ? "HIT" : "MISS";
        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] :
                           cacheable.cacheNames().length > 0 ? cacheable.cacheNames()[0] : "default";

        recordCacheEntry(operation, cacheName, buildKey(joinPoint), durationMs);
        return result;
    }

    @Around("@annotation(cacheEvict)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        if (!storage.isEnabled()) return joinPoint.proceed();

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        String cacheName = cacheEvict.value().length > 0 ? cacheEvict.value()[0] :
                           cacheEvict.cacheNames().length > 0 ? cacheEvict.cacheNames()[0] : "default";

        recordCacheEntry("EVICT", cacheName, buildKey(joinPoint), duration);
        return result;
    }

    @Around("@annotation(cachePut)")
    public Object aroundCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        if (!storage.isEnabled()) return joinPoint.proceed();

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        String cacheName = cachePut.value().length > 0 ? cachePut.value()[0] :
                           cachePut.cacheNames().length > 0 ? cachePut.cacheNames()[0] : "default";

        recordCacheEntry("PUT", cacheName, buildKey(joinPoint), duration);
        return result;
    }

    private void recordCacheEntry(String operation, String cacheName, String key, long duration) {
        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("operation", operation);
            content.put("cacheName", cacheName);
            content.put("key", key);
            content.put("duration", duration);

            List<String> tags = new ArrayList<>();
            tags.add("cache:" + operation.toLowerCase());
            tags.add("cache:" + cacheName);

            String userIdentifier = null;
            String tenantId = null;
            try {
                userIdentifier = userProvider.getCurrentUserIdentifier();
                tenantId = userProvider.getCurrentTenantId();
            } catch (Exception ignored) {
            }

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.CACHE)
                    .createdAt(LocalDateTime.now())
                    .batchId(TelescopeBatchContext.get())
                    .content(content)
                    .userIdentifier(userIdentifier)
                    .tenantId(tenantId)
                    .tags(tags)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return method;
        return method + " [" + Arrays.stream(args)
                .map(a -> a != null ? a.toString() : "null")
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
    }
}
