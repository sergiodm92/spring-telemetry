package dev.springtelescope.watcher;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class TelescopeHibernateIntegrator implements Integrator {

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {
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
