package com.tn.gias.transport.rasp.jdbc;

import com.tn.gias.transport.rasp.core.RaspGuard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;

/**
 * Wraps a java.sql.Connection obtained from the (proxied) DataSource.
 * Checks SQL text at prepareStatement/prepareCall time — the earliest
 * point the full query text is available — and wraps any returned
 * Statement/PreparedStatement/CallableStatement so raw Statement.execute*
 * calls are also checked at execution time.
 */
class RaspConnectionInvocationHandler implements InvocationHandler {

    private static final Set<String> PREPARE_METHODS = Set.of("prepareStatement", "prepareCall");
    private static final Set<String> STATEMENT_FACTORY_METHODS = Set.of(
            "createStatement", "prepareStatement", "prepareCall");

    private final Connection target;
    private final RaspGuard guard;

    RaspConnectionInvocationHandler(Connection target, RaspGuard guard) {
        this.target = target;
        this.guard = guard;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if (args != null && args.length > 0 && args[0] instanceof String sql && PREPARE_METHODS.contains(name)) {
            guard.checkSql("JDBC", sql);
        }

        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }

        if (STATEMENT_FACTORY_METHODS.contains(name) && result instanceof Statement statement) {
            return RaspProxyFactory.wrapStatement(statement, guard);
        }
        return result;
    }
}
