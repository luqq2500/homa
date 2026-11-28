package com.homa.rating.usecase;

import com.homa.rating.usecase.port.out.UnitOfWork;
import java.util.function.Supplier;

/** Test only: runs the work at once and counts the units, so a test can see one was used per session. */
public class InlineUnitOfWork implements UnitOfWork {

    private int units;

    @Override
    public <T> T inOwnTransaction(Supplier<T> work) {
        units++;
        return work.get();
    }

    public int units() {
        return units;
    }
}
