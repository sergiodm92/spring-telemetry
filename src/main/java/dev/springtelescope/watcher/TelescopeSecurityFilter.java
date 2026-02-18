package dev.springtelescope.watcher;

import dev.springtelescope.TelescopeProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects Telescope endpoints with a configurable access token.
 * Activate by setting {@code telescope.access-token} in application properties.
 * The token must be sent as a query parameter {@code ?token=...} or
 * as a header {@code X-Telescope-Token: ...}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TelescopeSecurityFilter extends OncePerRequestFilter {

    private final String basePath;
    private final String accessToken;

    public TelescopeSecurityFilter(TelescopeProperties properties) {
        this.basePath = properties.getBasePath();
        this.accessToken = properties.getAccessToken();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        if (!path.startsWith(basePath)) {
            chain.doFilter(request, response);
            return;
        }

        if (accessToken == null || accessToken.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String tokenParam = request.getParameter("token");
        String tokenHeader = request.getHeader("X-Telescope-Token");

        if (accessToken.equals(tokenParam) || accessToken.equals(tokenHeader)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Telescope access denied. Provide a valid token.\"}");
        }
    }
}
