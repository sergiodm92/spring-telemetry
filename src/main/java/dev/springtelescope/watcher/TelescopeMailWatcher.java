package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.*;

@Aspect
public class TelescopeMailWatcher {

    private final TelescopeStorage storage;
    private final TelescopeUserProvider userProvider;

    public TelescopeMailWatcher(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        this.storage = storage;
        this.userProvider = userProvider;
    }

    @Around("execution(* org.springframework.mail.MailSender.send(..))")
    public Object aroundSend(ProceedingJoinPoint joinPoint) throws Throwable {
        if (storage.isEnabled() && joinPoint.getArgs() != null) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof SimpleMailMessage msg) {
                    recordMail(msg);
                } else if (arg instanceof SimpleMailMessage[] msgs) {
                    for (SimpleMailMessage msg : msgs) {
                        recordMail(msg);
                    }
                }
            }
        }
        return joinPoint.proceed();
    }

    private void recordMail(SimpleMailMessage msg) {
        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("to", msg.getTo() != null ? String.join(", ", msg.getTo()) : null);
            content.put("from", msg.getFrom());
            content.put("subject", msg.getSubject());

            String body = msg.getText();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "... [truncated]";
            }
            content.put("bodyPreview", body);

            List<String> tags = new ArrayList<>();
            tags.add("mail");
            if (msg.getTo() != null && msg.getTo().length > 0) {
                tags.add("to:" + msg.getTo()[0]);
            }

            String userIdentifier = null;
            String tenantId = null;
            try {
                userIdentifier = userProvider.getCurrentUserIdentifier();
                tenantId = userProvider.getCurrentTenantId();
            } catch (Exception ignored) {
            }

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.MAIL)
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
}
