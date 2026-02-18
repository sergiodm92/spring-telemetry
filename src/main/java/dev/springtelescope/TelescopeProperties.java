package dev.springtelescope;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Data
@ConfigurationProperties(prefix = "telescope")
public class TelescopeProperties {
    private boolean enabled = true;
    private int maxEntries = 1000;
    private int pruneHours = 24;
    private long pruneIntervalMs = 3600000;
    private String basePath = "/telescope";
    private String basePackage = "";
    private Set<String> ignoredPrefixes = Set.of("/actuator", "/swagger", "/v3/api-docs");
    private String tenantPattern = "";
    private String accessToken = "";
    private String storage = "memory";
    private long flushIntervalMs = 2000;
    private Watchers watchers = new Watchers();

    @Data
    public static class Watchers {
        private boolean requests = true;
        private boolean queries = true;
        private boolean exceptions = true;
        private boolean logs = true;
        private boolean schedules = true;
        private boolean cache = true;
        private boolean events = true;
        private boolean mail = true;
        private boolean models = true;
    }
}
