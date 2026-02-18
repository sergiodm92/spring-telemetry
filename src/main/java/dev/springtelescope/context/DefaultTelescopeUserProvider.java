package dev.springtelescope.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation that uses Spring Security's SecurityContextHolder.
 * Falls back to null when Spring Security is not on the classpath.
 */
public class DefaultTelescopeUserProvider implements TelescopeUserProvider {

    private final Pattern tenantPattern;

    public DefaultTelescopeUserProvider(String tenantPatternStr) {
        this.tenantPattern = (tenantPatternStr != null && !tenantPatternStr.isBlank())
                ? Pattern.compile(tenantPatternStr)
                : null;
    }

    @Override
    public String getCurrentUserIdentifier() {
        try {
            // Use reflection to avoid hard dependency on Spring Security
            Class<?> securityContextHolderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object securityContext = securityContextHolderClass.getMethod("getContext").invoke(null);
            Object authentication = securityContext.getClass().getMethod("getAuthentication").invoke(securityContext);
            if (authentication == null) return null;

            Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
            if (principal == null) return null;

            // Check if principal is UserDetails
            Class<?> userDetailsClass = Class.forName("org.springframework.security.core.userdetails.UserDetails");
            if (userDetailsClass.isInstance(principal)) {
                return (String) userDetailsClass.getMethod("getUsername").invoke(principal);
            }

            // Fallback: use toString if not "anonymousUser"
            String str = principal.toString();
            return "anonymousUser".equals(str) ? null : str;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getCurrentTenantId() {
        if (tenantPattern == null) return null;
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String uri = request.getRequestURI();
                Matcher matcher = tenantPattern.matcher(uri);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
