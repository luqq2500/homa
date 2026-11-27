package com.homa.reservation.usecase.port.out;

import java.time.Instant;

/** Tells a provider that a job it hasn't marked Completed has ended, until Notification takes this over (JOB-5). */
public interface CompletionReminderNotifier {

    void remindToMarkCompleted(String reservationId, String providerId, Instant jobEnd, Instant reminderAt);
}
