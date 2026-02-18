package dev.springtelescope.watcher;

import dev.springtelescope.TelescopeProperties;
import dev.springtelescope.context.TelescopeBatchContext;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TelescopeRequestFilter extends OncePerRequestFilter {

    private final TelescopeStorage storage;
    private final Set<String> ignoredPrefixes;

    public TelescopeRequestFilter(TelescopeStorage storage, TelescopeProperties properties) {
        this.storage = storage;
        Set<String> prefixes = new HashSet<>(properties.getIgnoredPrefixes());
        prefixes.add(properties.getBasePath());
        this.ignoredPrefixes = Collections.unmodifiableSet(prefixes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        if (shouldIgnore(path) || !storage.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String batchId = UUID.randomUUID().toString();
        TelescopeBatchContext.set(batchId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String capturedUser = (String) wrappedRequest.getAttribute("telescope.userIdentifier");
            String capturedTenant = (String) wrappedRequest.getAttribute("telescope.tenantId");
            recordRequest(wrappedRequest, wrappedResponse, batchId, duration, capturedUser, capturedTenant);
            wrappedResponse.copyBodyToResponse();
            TelescopeBatchContext.clear();
        }
    }

    private void recordRequest(ContentCachingRequestWrapper request,
                               ContentCachingResponseWrapper response,
                               String batchId, long duration,
                               String userIdentifier, String tenantId) {
        try {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("method", request.getMethod());
            content.put("uri", request.getRequestURI());
            content.put("queryString", request.getQueryString());
            content.put("status", response.getStatus());
            content.put("duration", duration);
            content.put("ipAddress", request.getRemoteAddr());
            content.put("contentType", request.getContentType());
            content.put("responseContentType", response.getContentType());
            content.put("requestHeaders", extractHeaders(request));
            content.put("responseHeaders", extractResponseHeaders(response));

            String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (requestBody.length() > 10000) {
                requestBody = requestBody.substring(0, 10000) + "... [truncated]";
            }
            content.put("requestBody", requestBody);

            String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (responseBody.length() > 10000) {
                responseBody = responseBody.substring(0, 10000) + "... [truncated]";
            }
            content.put("responseBody", responseBody);

            storage.store(TelescopeEntry.builder()
                    .uuid(UUID.randomUUID().toString())
                    .type(TelescopeEntryType.REQUEST)
                    .createdAt(LocalDateTime.now())
                    .batchId(batchId)
                    .content(content)
                    .userIdentifier(userIdentifier)
                    .tenantId(tenantId)
                    .build());
        } catch (Exception ignored) {
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) return headers;
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.equalsIgnoreCase("authorization") || name.equalsIgnoreCase("cookie")) {
                headers.put(name, "***");
            } else {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }

    private Map<String, String> extractResponseHeaders(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, response.getHeader(name));
        }
        return headers;
    }

    private boolean shouldIgnore(String path) {
        return ignoredPrefixes.stream().anyMatch(path::startsWith);
    }
}
