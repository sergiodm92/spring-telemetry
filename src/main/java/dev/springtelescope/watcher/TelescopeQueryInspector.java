package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TelescopeQueryInspector implements StatementInspector {

    private final TelescopeStorage storage;
    private final TelescopeUserProvider userProvider;

    public TelescopeQueryInspector(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        this.storage = storage;
        this.userProvider = userProvider;
    }

    @Override
    public String inspect(String sql) {
        if (storage == null || !storage.isEnabled() || sql == null) return sql;

        String trimmedSql = sql.trim().toLowerCase();
        if (trimmedSql.startsWith("select 1") || trimmedSql.startsWith("select version")) {
            return sql;
        }

        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("sql", sql.trim());
            content.put("type", getSqlType(trimmedSql));

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
                    .type(TelescopeEntryType.QUERY)
                    .createdAt(LocalDateTime.now())
                    .batchId(TelescopeBatchContext.get())
                    .content(content)
                    .userIdentifier(userIdentifier)
                    .tenantId(tenantId)
                    .build());
        } catch (Exception ignored) {
        }

        return sql;
    }

    private String getSqlType(String sql) {
        if (sql.startsWith("select")) return "SELECT";
        if (sql.startsWith("insert")) return "INSERT";
        if (sql.startsWith("update")) return "UPDATE";
        if (sql.startsWith("delete")) return "DELETE";
        if (sql.startsWith("create") || sql.startsWith("alter") || sql.startsWith("drop")) return "DDL";
        return "OTHER";
    }
}
