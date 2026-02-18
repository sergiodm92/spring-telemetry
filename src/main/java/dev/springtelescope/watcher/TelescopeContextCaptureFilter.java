package dev.springtelescope.watcher;

import dev.springtelescope.context.TelescopeUserProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Captures user context as request attributes while the SecurityContext is still available.
 * Runs after the security filter chain (Order=2) so authentication has already been set up.
 * The TelescopeRequestFilter reads these attributes in its finally block.
 */
@Order(2)
public class TelescopeContextCaptureFilter extends OncePerRequestFilter {

    private final TelescopeUserProvider userProvider;

    public TelescopeContextCaptureFilter(TelescopeUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userIdentifier = userProvider.getCurrentUserIdentifier();
        if (userIdentifier != null) {
            request.setAttribute("telescope.userIdentifier", userIdentifier);
        }
        String tenantId = userProvider.getCurrentTenantId();
        if (tenantId != null) {
            request.setAttribute("telescope.tenantId", tenantId);
        }
        chain.doFilter(request, response);
    }
}
