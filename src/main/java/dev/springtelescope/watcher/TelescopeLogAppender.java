package dev.springtelescope.watcher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TelescopeLogAppender extends AppenderBase<ILoggingEvent> {

    private static volatile TelescopeStorage storage;
    private static volatile TelescopeUserProvider userProvider;
    private static volatile String basePackage = "";

    public static void configure(TelescopeStorage storage, TelescopeUserProvider userProvider, String basePackage) {
        TelescopeLogAppender.storage = storage;
        TelescopeLogAppender.userProvider = userProvider;
        TelescopeLogAppender.basePackage = (basePackage != null) ? basePackage : "";
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (storage == null || !storage.isEnabled()) return;

        if (event.getLevel().toInt() < Level.INFO_INT) return;

        String loggerName = event.getLoggerName();

        // Skip telescope's own logs to avoid infinite loops
        if (loggerName.startsWith("dev.springtelescope")) return;

        // Only capture application logs if basePackage is configured
        if (!basePackage.isEmpty() && !loggerName.startsWith(basePackage)) return;

        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("level", event.getLevel().toString());
            content.put("message", event.getFormattedMessage());
            content.put("logger", loggerName);
            content.put("thread", event.getThreadName());

            if (event.getThrowableProxy() != null) {
                content.put("exception", ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }

            if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
                content.put("mdc", new LinkedHashMap<>(event.getMDCPropertyMap()));
            }

            String userIdentifier = null;
            String tenantId = null;
            if (userProvider != null) {
                try {
                    userIdentifier = userProvider.getCurrentUserIdentifier();
                    tenantId = userProvider.getCurrentTenantId();
                } catch (Exception ignored) {
                }
            }

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.LOG)
                    .createdAt(LocalDateTime.now())
                    .batchId(TelescopeBatchContext.get())
                    .content(content)
                    .userIdentifier(userIdentifier)
                    .tenantId(tenantId)
                    .build());
        } catch (Exception ignored) {
        }
    }
}
