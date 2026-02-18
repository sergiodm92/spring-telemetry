package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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
    public Object aroundSimpleSend(ProceedingJoinPoint joinPoint) throws Throwable {
        if (storage.isEnabled() && joinPoint.getArgs() != null) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof SimpleMailMessage msg) {
                    recordSimpleMail(msg);
                } else if (arg instanceof SimpleMailMessage[] msgs) {
                    for (SimpleMailMessage msg : msgs) {
                        recordSimpleMail(msg);
                    }
                }
            }
        }
        return joinPoint.proceed();
    }

    @Around("execution(* org.springframework.mail.javamail.JavaMailSender.send(..))")
    public Object aroundMimeSend(ProceedingJoinPoint joinPoint) throws Throwable {
        if (storage.isEnabled() && joinPoint.getArgs() != null) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof MimeMessage msg) {
                    recordMimeMessage(msg);
                } else if (arg instanceof MimeMessage[] msgs) {
                    for (MimeMessage msg : msgs) {
                        recordMimeMessage(msg);
                    }
                }
            }
        }
        return joinPoint.proceed();
    }

    private void recordSimpleMail(SimpleMailMessage msg) {
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
            content.put("type", "simple");

            List<String> tags = new ArrayList<>();
            tags.add("mail");
            if (msg.getTo() != null && msg.getTo().length > 0) {
                tags.add("to:" + msg.getTo()[0]);
            }

            storeMailEntry(content, tags);
        } catch (Exception ignored) {
        }
    }

    private void recordMimeMessage(MimeMessage msg) {
        try {
            Map<String, Object> content = new LinkedHashMap<>();

            // Extract recipients
            jakarta.mail.Address[] toAddresses = msg.getRecipients(Message.RecipientType.TO);
            if (toAddresses != null) {
                StringBuilder to = new StringBuilder();
                for (jakarta.mail.Address addr : toAddresses) {
                    if (!to.isEmpty()) to.append(", ");
                    if (addr instanceof InternetAddress ia) {
                        to.append(ia.getAddress());
                    } else {
                        to.append(addr.toString());
                    }
                }
                content.put("to", to.toString());
            }

            // Extract from
            jakarta.mail.Address[] fromAddresses = msg.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                if (fromAddresses[0] instanceof InternetAddress ia) {
                    content.put("from", ia.getAddress());
                } else {
                    content.put("from", fromAddresses[0].toString());
                }
            }

            content.put("subject", msg.getSubject());
            content.put("contentType", msg.getContentType());
            content.put("type", "mime");

            List<String> tags = new ArrayList<>();
            tags.add("mail");
            tags.add("mime");
            if (toAddresses != null && toAddresses.length > 0) {
                String firstTo = toAddresses[0] instanceof InternetAddress ia
                        ? ia.getAddress() : toAddresses[0].toString();
                tags.add("to:" + firstTo);
            }

            storeMailEntry(content, tags);
        } catch (Exception ignored) {
        }
    }

    private void storeMailEntry(Map<String, Object> content, List<String> tags) {
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
    }
}
