package dev.springtelescope.watcher;

import dev.springtelescope.TelescopeProperties;
import dev.springtelescope.storage.TelescopeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
public class TelescopePruner {

    private final TelescopeStorage storage;
    private final TelescopeProperties properties;

    public TelescopePruner(TelescopeStorage storage, TelescopeProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${telescope.prune-interval-ms:3600000}")
    public void prune() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(properties.getPruneHours());
        long pruned = storage.pruneOlderThan(cutoff);
        if (pruned > 0) {
            log.debug("Telescope pruned {} entries older than {} hours", pruned, properties.getPruneHours());
        }
    }
}
