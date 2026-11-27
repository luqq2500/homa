package com.homa.reservation.usecase.port.out;

import java.time.Instant;

/** The completion reminders already sent, so the scheduler sends each one once (JOB-5). */
public interface CompletionReminderLog {

    /** True the first time this reminder of this reservation is recorded; false when it was sent already. */
    boolean recordFirstSending(String reservationId, Instant reminderAt);
}
