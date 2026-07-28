package com.tn.gias.transport.rasp.jdbc;

import com.tn.gias.transport.rasp.core.RaspGuard;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;

final class RaspProxyFactory {

    private RaspProxyFactory() {
    }

    static Statement wrapStatement(Statement target, RaspGuard guard) {
        Class<?> iface = target instanceof CallableStatement ? CallableStatement.class
                : target instanceof PreparedStatement ? PreparedStatement.class
                : Statement.class;
        return (Statement) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                new RaspSqlStatementInvocationHandler(target, guard));
    }
}
