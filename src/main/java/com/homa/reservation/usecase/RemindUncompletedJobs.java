package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.RemindUncompletedJobsRequest;
import com.homa.reservation.usecase.dto.RemindUncompletedJobsResponse;
import com.homa.reservation.usecase.port.in.UncompletedJobReminder;
import com.homa.reservation.usecase.port.out.CompletionReminderLog;
import com.homa.reservation.usecase.port.out.CompletionReminderNotifier;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * JOB-5, run by a scheduler: a job not marked Completed by its end reminds the provider at the job end and 2 hours
 * later. Each reminder is sent once, however often the scheduler runs; the payout waits until the job is marked.
 */
public final class RemindUncompletedJobs implements UncompletedJobReminder {

    private final ReservationRepository reservations;
    private final CompletionReminderLog sent;
    private final CompletionReminderNotifier notifier;
    private final TimeSource time;

    public RemindUncompletedJobs(ReservationRepository reservations, CompletionReminderLog sent,
            CompletionReminderNotifier notifier, TimeSource time) {
        this.reservations = reservations;
        this.sent = sent;
        this.notifier = notifier;
        this.time = time;
    }

    @Override
    public RemindUncompletedJobsResponse remind(RemindUncompletedJobsRequest request) {
        Instant now = time.now();
        int remindersSent = 0;
        for (Reservation reservation : reservations.findReservedEndedBy(now)) {
            for (Instant reminder : reservation.completionRemindersDueAt(now)) {
                remindersSent += send(reservation, reminder);
            }
        }
        return new RemindUncompletedJobsResponse(remindersSent);
    }

    private int send(Reservation reservation, Instant reminder) {
        if (!sent.recordFirstSending(reservation.id().value(), reminder)) {
            return 0;
        }
        notifier.remindToMarkCompleted(reservation.id().value(), reservation.booking().providerId(),
                reservation.jobTime().end(), reminder);
        return 1;
    }
}
