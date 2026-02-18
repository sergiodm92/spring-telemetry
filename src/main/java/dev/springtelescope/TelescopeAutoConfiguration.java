package dev.springtelescope;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import dev.springtelescope.context.DefaultTelescopeUserProvider;
import dev.springtelescope.context.TelescopeUserProvider;
import dev.springtelescope.controller.TelescopeController;
import dev.springtelescope.filter.DefaultTelescopeFilterProvider;
import dev.springtelescope.filter.TelescopeFilterProvider;
import dev.springtelescope.storage.TelescopeStorage;
import dev.springtelescope.watcher.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnProperty(prefix = "telescope", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TelescopeProperties.class)
@EnableScheduling
@Import(TelescopeController.class)
public class TelescopeAutoConfiguration {

    private final TelescopeProperties properties;

    public TelescopeAutoConfiguration(TelescopeProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public TelescopeStorage telescopeStorage() {
        return new TelescopeStorage(properties.getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean
    public TelescopeUserProvider telescopeUserProvider() {
        return new DefaultTelescopeUserProvider(properties.getTenantPattern());
    }

    @Bean
    @ConditionalOnMissingBean
    public TelescopeFilterProvider telescopeFilterProvider(TelescopeStorage storage) {
        return new DefaultTelescopeFilterProvider(storage);
    }

    // --- Watchers ---

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "requests", havingValue = "true", matchIfMissing = true)
    public TelescopeRequestFilter telescopeRequestFilter(TelescopeStorage storage) {
        return new TelescopeRequestFilter(storage, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "requests", havingValue = "true", matchIfMissing = true)
    public TelescopeContextCaptureFilter telescopeContextCaptureFilter(TelescopeUserProvider userProvider) {
        return new TelescopeContextCaptureFilter(userProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "exceptions", havingValue = "true", matchIfMissing = true)
    public TelescopeExceptionRecorder telescopeExceptionRecorder(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return new TelescopeExceptionRecorder(storage, userProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "events", havingValue = "true", matchIfMissing = true)
    public TelescopeEventWatcher telescopeEventWatcher(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return new TelescopeEventWatcher(storage, userProvider, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "schedules", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    public TelescopeScheduleAspect telescopeScheduleAspect(TelescopeStorage storage) {
        return new TelescopeScheduleAspect(storage);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "cache", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    public TelescopeCacheAspect telescopeCacheAspect(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return new TelescopeCacheAspect(storage, userProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "mail", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = {"org.aspectj.lang.ProceedingJoinPoint", "org.springframework.mail.MailSender"})
    public TelescopeMailWatcher telescopeMailWatcher(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return new TelescopeMailWatcher(storage, userProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "queries", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.hibernate.SessionFactory")
    public HibernatePropertiesCustomizer telescopeHibernateCustomizer(TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return hibernateProperties -> hibernateProperties.put(
                "hibernate.session_factory.statement_inspector",
                new TelescopeQueryInspector(storage, userProvider)
        );
    }

    @Bean
    public TelescopePruner telescopePruner(TelescopeStorage storage) {
        return new TelescopePruner(storage, properties);
    }

    @Bean
    public WebMvcConfigurer telescopeViewConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                String basePath = properties.getBasePath();
                registry.addRedirectViewController(basePath, basePath + "/index.html");
                registry.addRedirectViewController(basePath + "/", basePath + "/index.html");
            }
        };
    }

    // --- Log appender and model listener initialization ---

    @PostConstruct
    public void initStaticComponents() {
        // Initialize TelescopeModelListener static storage
        try {
            Class.forName("org.hibernate.event.spi.PostInsertEventListener");
            // Only configure if Hibernate is on the classpath
            TelescopeStorage storage = telescopeStorage();
            TelescopeUserProvider userProvider = telescopeUserProvider();
            TelescopeModelListener.configure(storage, userProvider);
        } catch (ClassNotFoundException ignored) {
            // Hibernate not on classpath, skip
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "telescope.watchers", name = "logs", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
    public TelescopeLogAppenderInitializer telescopeLogAppenderInitializer(
            TelescopeStorage storage, TelescopeUserProvider userProvider) {
        return new TelescopeLogAppenderInitializer(storage, userProvider, properties.getBasePackage());
    }

    public static class TelescopeLogAppenderInitializer {

        public TelescopeLogAppenderInitializer(TelescopeStorage storage, TelescopeUserProvider userProvider, String basePackage) {
            TelescopeLogAppender.configure(storage, userProvider, basePackage);

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            TelescopeLogAppender appender = new TelescopeLogAppender();
            appender.setContext(loggerContext);
            appender.setName("TELESCOPE");
            appender.start();

            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(appender);
        }
    }
}
