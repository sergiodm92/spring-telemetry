package dev.springtelescope.watcher;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hibernate SPI integrator that registers the {@link TelescopeModelListener}
 * for entity change tracking (INSERT, UPDATE, DELETE).
 * <p>
 * This integrator is discovered via {@code META-INF/services/org.hibernate.integrator.spi.Integrator}.
 * It only registers listeners if {@link TelescopeModelListener#isConfigured()} returns true,
 * meaning the Spring auto-configuration has initialized it with a storage and user provider.
 */
public class TelescopeHibernateIntegrator implements Integrator {

    @SuppressWarnings("deprecation")
    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {
        if (!TelescopeModelListener.isConfigured()) {
            return;
        }

        EventListenerRegistry registry = serviceRegistry.getService(EventListenerRegistry.class);
        TelescopeModelListener listener = new TelescopeModelListener();

        registry.appendListeners(EventType.POST_INSERT, listener);
        registry.appendListeners(EventType.POST_UPDATE, listener);
        registry.appendListeners(EventType.POST_DELETE, listener);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory,
                             SessionFactoryServiceRegistry serviceRegistry) {
        // no-op
    }
}
