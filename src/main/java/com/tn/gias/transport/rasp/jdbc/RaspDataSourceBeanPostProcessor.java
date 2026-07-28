package com.tn.gias.transport.rasp.jdbc;

import com.tn.gias.transport.rasp.RaspProperties;
import com.tn.gias.transport.rasp.core.RaspGuard;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import javax.sql.DataSource;

/**
 * Layer 2 of RASP (defense in depth): transparently wraps the application's
 * DataSource bean(s) so every SQL statement — including ones built by
 * native queries / JdbcTemplate / EntityManager.createNativeQuery, which
 * are the realistic SQL-injection surface in a JPA app — is inspected right
 * before it reaches the database driver, independent of whether the attack
 * payload was already caught (or missed, or reached this code via a path
 * that never goes through the HTTP filter) upstream.
 *
 * Deliberately NOT PriorityOrdered/Ordered: Spring instantiates
 * PriorityOrdered BeanPostProcessors in a first pass, one by one, before any
 * of them (including AutowiredAnnotationBeanPostProcessor) are registered as
 * active — so a PriorityOrdered BPP with a parameterized constructor cannot
 * benefit from constructor autowiring and fails with "no default
 * constructor found". Staying unordered defers this bean's creation until
 * after that first pass completes, once autowiring is available again.
 */
@Component
public class RaspDataSourceBeanPostProcessor implements BeanPostProcessor {

    private final RaspGuard guard;
    private final RaspProperties properties;

    public RaspDataSourceBeanPostProcessor(RaspGuard guard, RaspProperties properties) {
        this.guard = guard;
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!properties.isEnabled() || !(bean instanceof DataSource dataSource) || Proxy.isProxyClass(bean.getClass())) {
            return bean;
        }
        return Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                new RaspDataSourceInvocationHandler(dataSource, guard));
    }
}
