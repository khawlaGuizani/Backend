package com.tn.gias.transport.rasp.jdbc;

import com.tn.gias.transport.rasp.core.RaspGuard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Wraps a java.sql.Statement / PreparedStatement / CallableStatement.
 * For plain Statement, the SQL text is passed as an argument to
 * execute/executeQuery/executeUpdate — checked here right before execution.
 * For PreparedStatement/CallableStatement, the SQL text was already checked
 * by RaspConnectionInvocationHandler when the statement was created, so
 * these no-arg execute() calls simply pass through.
 */
class RaspSqlStatementInvocationHandler implements InvocationHandler {

    private static final Set<String> SQL_EXECUTING_METHODS = Set.of(
            "execute", "executeQuery", "executeUpdate", "executeLargeUpdate", "addBatch");

    private final Object target;
    private final RaspGuard guard;

    RaspSqlStatementInvocationHandler(Object target, RaspGuard guard) {
        this.target = target;
        this.guard = guard;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null && args.length > 0 && args[0] instanceof String sql
                && SQL_EXECUTING_METHODS.contains(method.getName())) {
            guard.checkSql("JDBC", sql);
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }
}
