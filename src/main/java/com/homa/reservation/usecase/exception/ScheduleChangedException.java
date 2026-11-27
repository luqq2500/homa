package com.homa.reservation.usecase.exception;

/**
 * A schedule was saved by someone else after it was loaded (the optimistic lock), so nothing was saved. The use case
 * reloads and asks again; the retry sees the other save, for example the block of a payment confirmed first
 * (RES-11).
 */
public final class ScheduleChangedException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ScheduleChangedException(String scheduleOwnerId) {
        super("The schedule of " + scheduleOwnerId + " changed since it was loaded");
    }
}
