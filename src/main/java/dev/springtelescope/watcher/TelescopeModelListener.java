package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;

import java.time.LocalDateTime;
import java.util.*;

public class TelescopeModelListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static volatile TelescopeStorage storage;
    private static volatile TelescopeUserProvider userProvider;

    public static void configure(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        TelescopeModelListener.storage = storage;
        TelescopeModelListener.userProvider = userProvider;
    }

    public static boolean isConfigured() {
        return storage != null;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        record("CREATED", event.getEntity(), event.getId(), event.getPersister(), null);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        List<String> changedFields = detectChangedFields(event);
        record("UPDATED", event.getEntity(), event.getId(), event.getPersister(), changedFields);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        record("DELETED", event.getEntity(), event.getId(), event.getPersister(), null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private void record(String action, Object entity, Object entityId, EntityPersister persister,
                        List<String> changedFields) {
        if (storage == null || !storage.isEnabled()) return;

        String entityClass = entity.getClass().getName();

        // Skip telescope's own entities
        if (entityClass.contains(".telescope.") || entityClass.startsWith("dev.springtelescope")) return;

        try {
            String entityName = entity.getClass().getSimpleName();

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("entity", entityName);
            content.put("entityClass", entityClass);
            content.put("action", action);
            content.put("entityId", entityId != null ? entityId.toString() : null);
            if (changedFields != null && !changedFields.isEmpty()) {
                content.put("changedFields", changedFields);
            }

            List<String> tags = new ArrayList<>();
            tags.add("model:" + action.toLowerCase());
            tags.add("entity:" + entityName);

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
                    .type(TelescopeEntryType.MODEL)
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

    private List<String> detectChangedFields(PostUpdateEvent event) {
        List<String> changed = new ArrayList<>();
        try {
            String[] propertyNames = event.getPersister().getPropertyNames();
            Object[] oldState = event.getOldState();
            Object[] newState = event.getState();
            if (oldState != null && newState != null) {
                for (int i = 0; i < propertyNames.length; i++) {
                    if (!Objects.equals(oldState[i], newState[i])) {
                        changed.add(propertyNames[i]);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return changed;
    }
}
