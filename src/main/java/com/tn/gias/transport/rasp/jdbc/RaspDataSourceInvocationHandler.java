package com.tn.gias.transport.rasp.jdbc;

import com.tn.gias.transport.rasp.core.RaspGuard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import javax.sql.DataSource;

class RaspDataSourceInvocationHandler implements InvocationHandler {

    private final DataSource target;
    private final RaspGuard guard;

    RaspDataSourceInvocationHandler(DataSource target, RaspGuard guard) {
        this.target = target;
        this.guard = guard;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }

        if (result instanceof Connection connection) {
            return Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    new RaspConnectionInvocationHandler(connection, guard));
        }
        return result;
    }
}
