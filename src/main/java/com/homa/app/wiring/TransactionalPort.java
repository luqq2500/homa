package com.homa.app.wiring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wraps a use case behind its {@code port/in} interface so every call runs in one transaction (ADR 0002). Use case
 * code carries no Spring annotation; each context's wiring in {@code com.homa.app.wiring} builds the implementation
 * from its infra adapters and exposes only this wrapped port. A call that throws rolls the transaction back and
 * rethrows the original exception unchanged, so a domain or usecase refusal reaches the caller as itself.
 */
public final class TransactionalPort {

    private TransactionalPort() {
    }

    /** {@code target} behind {@code port}, each interface method in its own transaction. */
    public static <T> T wrap(Class<T> port, T target, PlatformTransactionManager transactions) {
        Objects.requireNonNull(target, "target");
        if (!port.isInterface()) {
            throw new IllegalArgumentException(port.getName() + " is not a port interface");
        }
        TransactionTemplate template = new TransactionTemplate(transactions);
        InvocationHandler handler = (proxy, method, args) -> invoke(template, target, method, args);
        return port.cast(Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[] {port}, handler));
    }

    private static Object invoke(TransactionTemplate template, Object target, Method method, Object[] args)
            throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        try {
            return template.execute(status -> call(target, method, args));
        } catch (Rethrown rethrown) {
            throw rethrown.getCause();
        }
    }

    private static Object call(Object target, Method method, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw new Rethrown(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Carries the use case's own exception through the template, which rolls back on any runtime exception. */
    private static final class Rethrown extends RuntimeException {

        private static final long serialVersionUID = 1L;

        Rethrown(Throwable cause) {
            super(cause);
        }
    }
}
