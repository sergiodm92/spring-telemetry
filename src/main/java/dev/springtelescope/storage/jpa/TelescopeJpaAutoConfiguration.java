package dev.springtelescope.storage.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springtelescope.storage.TelescopeStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@ConditionalOnProperty(prefix = "telescope", name = "storage", havingValue = "database")
@AutoConfigureBefore(name = "dev.springtelescope.TelescopeAutoConfiguration")
@EntityScan(basePackageClasses = TelescopeEntryEntity.class)
@EnableJpaRepositories(basePackageClasses = TelescopeEntryRepository.class)
public class TelescopeJpaAutoConfiguration {

    @Bean
    public TelescopeStorage telescopeStorage(TelescopeEntryRepository repository, ObjectMapper objectMapper) {
        return new JpaTelescopeStorage(repository, objectMapper);
    }

    @Bean
    public TelescopeStorageFlusher telescopeStorageFlusher(TelescopeStorage storage, TelescopeEntryRepository repository) {
        return new TelescopeStorageFlusher((JpaTelescopeStorage) storage, repository);
    }
}
