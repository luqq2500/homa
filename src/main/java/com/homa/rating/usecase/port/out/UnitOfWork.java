package com.homa.rating.usecase.port.out;

import java.util.function.Supplier;

/**
 * Runs one piece of work in a transaction of its own, committed or rolled back on its own whatever transaction the
 * caller is in. A scheduler that visits many aggregates uses it so that one failing or conflicting aggregate never
 * undoes the others, and so that two runs at once each hold an aggregate only for its own work (RTG-3).
 */
public interface UnitOfWork {

    /** Runs {@code work} in its own transaction and returns its result. A failure rolls back and is rethrown. */
    <T> T inOwnTransaction(Supplier<T> work);
}
