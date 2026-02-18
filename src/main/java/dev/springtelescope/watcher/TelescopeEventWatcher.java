package dev.springtelescope.watcher;

import dev.springtelescope.TelescopeProperties;
import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

public class TelescopeEventWatcher {

    private final TelescopeStorage storage;
    private final TelescopeUserProvider userProvider;
    private final String basePackage;

    public TelescopeEventWatcher(TelescopeStorage storage, TelescopeUserProvider userProvider, TelescopeProperties properties) {
        this.storage = storage;
        this.userProvider = userProvider;
        this.basePackage = properties.getBasePackage();
    }

    @EventListener
    public void onApplicationEvent(ApplicationEvent event) {
        if (!storage.isEnabled()) return;

        String eventClass = event.getClass().getName();

        // Only capture domain events from the project
        if (basePackage != null && !basePackage.isEmpty() && !eventClass.startsWith(basePackage)) return;

        // Exclude telescope events to avoid loops
        if (eventClass.contains(".telescope.") || eventClass.startsWith("dev.springtelescope")) return;

        // Exclude Spring framework internal events
        if (eventClass.startsWith("org.springframework.")) return;

        try {
            String eventName = event.getClass().getSimpleName();

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("event", eventName);
            content.put("eventClass", eventClass);
            content.put("source", event.getSource() != null ? event.getSource().getClass().getSimpleName() : null);
            content.put("payload", extractPayload(event));

            List<String> tags = new ArrayList<>();
            tags.add("event:" + eventName);

            String userIdentifier = null;
            String tenantId = null;
            try {
                userIdentifier = userProvider.getCurrentUserIdentifier();
                tenantId = userProvider.getCurrentTenantId();
            } catch (Exception ignored) {
            }

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.EVENT)
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

    private Map<String, Object> extractPayload(ApplicationEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        try {
            for (Method method : event.getClass().getDeclaredMethods()) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0
                        && !method.getName().equals("getClass")) {
                    method.setAccessible(true);
                    Object value = method.invoke(event);
                    String key = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
                    payload.put(key, value != null ? value.toString() : null);
                }
            }
            for (Method method : event.getClass().getMethods()) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0
                        && !method.getName().equals("getClass") && !method.getName().equals("getSource")
                        && !method.getName().equals("getTimestamp")
                        && !method.getDeclaringClass().getName().startsWith("org.springframework.")
                        && !method.getDeclaringClass().getName().startsWith("java.")
                        && !payload.containsKey(method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4))) {
                    Object value = method.invoke(event);
                    String key = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
                    payload.put(key, value != null ? value.toString() : null);
                }
            }
        } catch (Exception ignored) {
        }
        return payload;
    }
}
