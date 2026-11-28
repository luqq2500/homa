package com.homa.reservation.usecase;

import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.out.ReservationChangedException;
import java.util.function.Supplier;

/**
 * Runs a step that saves again when someone else saved a schedule or the reservation first (the optimistic lock).
 * Each attempt reloads, so a retry sees the other save: a payment confirmed second then finds the time taken
 * (RES-11), and a withdrawal that lost to an acceptance finds the reservation Reserved (REQ-5). After
 * {@link #ATTEMPTS} conflicts the exception is let through, the transaction rolls back and the step runs again later
 * (a handler's event is redelivered).
 */
public final class ScheduleRetry {

    static final int ATTEMPTS = 3;

    private ScheduleRetry() {
    }

    public static <T> T retried(Supplier<T> attempt) {
        for (int tried = 1; ; tried++) {
            try {
                return attempt.get();
            } catch (ScheduleChangedException | ReservationChangedException conflict) {
                if (tried == ATTEMPTS) {
                    throw conflict;
                }
            }
        }
    }
}
