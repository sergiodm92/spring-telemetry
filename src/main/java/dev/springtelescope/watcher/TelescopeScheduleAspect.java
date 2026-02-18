package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
public class TelescopeScheduleAspect {

    private final TelescopeStorage storage;

    public TelescopeScheduleAspect(TelescopeStorage storage) {
        this.storage = storage;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object aroundScheduled(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!storage.isEnabled()) {
            return joinPoint.proceed();
        }

        // Skip telescope's own scheduled tasks to avoid circular recording
        String targetClass = joinPoint.getTarget().getClass().getName();
        if (targetClass.startsWith("dev.springtelescope")) {
            return joinPoint.proceed();
        }

        String batchId = UUID.randomUUID().toString();
        TelescopeBatchContext.set(batchId);

        long startTime = System.currentTimeMillis();
        String status = "completed";
        String exception = null;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            status = "failed";
            exception = t.getClass().getSimpleName() + ": " + t.getMessage();
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("class", joinPoint.getTarget().getClass().getSimpleName());
            content.put("method", joinPoint.getSignature().getName());
            content.put("status", status);
            content.put("duration", duration);
            if (exception != null) {
                content.put("exception", exception);
            }

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.SCHEDULE)
                    .createdAt(LocalDateTime.now())
                    .batchId(batchId)
                    .content(content)
                    .build());

            TelescopeBatchContext.clear();
        }
    }
}
