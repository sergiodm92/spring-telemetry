package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TelescopeExceptionRecorder {

    private final TelescopeStorage storage;
    private final TelescopeUserProvider userProvider;

    public TelescopeExceptionRecorder(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        this.storage = storage;
        this.userProvider = userProvider;
    }

    public void record(Exception ex) {
        if (!storage.isEnabled()) return;

        try {
            HttpServletRequest request = getCurrentRequest();

            Map<String, Object> content = new LinkedHashMap<>();
            content.put("class", ex.getClass().getName());
            content.put("message", ex.getMessage());
            content.put("trace", getStackTrace(ex));

            if (ex.getStackTrace().length > 0) {
                StackTraceElement first = ex.getStackTrace()[0];
                content.put("file", first.getFileName());
                content.put("line", first.getLineNumber());
                content.put("location", first.getClassName() + "." + first.getMethodName());
            }

            if (request != null) {
                content.put("uri", request.getRequestURI());
                content.put("method", request.getMethod());
            }

            if (ex.getCause() != null) {
                content.put("cause", ex.getCause().getClass().getName() + ": " + ex.getCause().getMessage());
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
                    .type(TelescopeEntryType.EXCEPTION)
                    .createdAt(LocalDateTime.now())
                    .batchId(TelescopeBatchContext.get())
                    .content(content)
                    .userIdentifier(userIdentifier)
                    .tenantId(tenantId)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        if (trace.length() > 5000) {
            trace = trace.substring(0, 5000) + "\n... [truncated]";
        }
        return trace;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
