package dev.springtelescope.storage.jpa;

import dev.springtelescope.model.TelescopeEntry;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

public class TelescopeStorageFlusher {

    private final JpaTelescopeStorage storage;
    private final TelescopeEntryRepository repository;

    public TelescopeStorageFlusher(JpaTelescopeStorage storage, TelescopeEntryRepository repository) {
        this.storage = storage;
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${telescope.flush-interval-ms:2000}")
    public void flush() {
        drainBuffer();
    }

    @PreDestroy
    public void onShutdown() {
        drainBuffer();
    }

    private void drainBuffer() {
        List<TelescopeEntryEntity> batch = new ArrayList<>();
        TelescopeEntry entry;
        while ((entry = storage.getBuffer().poll()) != null) {
            batch.add(storage.toEntity(entry));
        }
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
        }
    }
}
