package com.homa.reservation.domain;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationValues.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The booked period from the job start time to the job end time (§1 "Job time"). It answers every time question
 * about a reservation, so no use case does date arithmetic. The Check-in Period, Protection Window and completion
 * reminders are Homa's reservation values, so each question takes the {@link Version} the reservation keeps (P3).
 */
public record JobTime(Instant start, JobDuration duration) {

    public JobTime {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(duration, "duration");
    }

    public static JobTime of(Instant start, Duration length) {
        return new JobTime(start, JobDuration.of(length));
    }

    public Instant end() {
        return start.plus(duration.length());
    }

    /** The job time itself, [start, end): what the customer's calendar blocks (RES-10). */
    public TimeInterval interval() {
        return new TimeInterval(start, end());
    }

    /** When the Check-in Period starts: 15 minutes before the job start time. */
    public Instant checkInStart(Version values) {
        return start.minus(values.checkInPeriod());
    }

    /** [Check-in Period start, start): the team arrives in this period (JOB-1). */
    public TimeInterval checkInPeriod(Version values) {
        return new TimeInterval(checkInStart(values), start);
    }

    /** [Check-in Period start, job end + 24 h). */
    public TimeInterval protectionWindow(Version values) {
        return new TimeInterval(checkInStart(values), end().plus(values.protectionAfterJobEnd()));
    }

    /** §1 "Notice": from now until the Check-in Period starts (RES-4). Negative once it has started. */
    public Duration noticeFrom(Instant now, Version values) {
        return Duration.between(now, checkInStart(values));
    }

    /** [Check-in Period start, job end + buffer): what the provider's calendar blocks (RES-9). */
    public TimeInterval calendarBlock(Duration buffer, Version values) {
        return new TimeInterval(checkInStart(values), end().plus(buffer));
    }

    /** JOB-5: the provider is reminded at the job end time and 2 hours later while the job isn't marked Completed. */
    public List<Instant> completionReminders(Version values) {
        return values.completionRemindersAfterJobEnd().stream().map(end()::plus).toList();
    }

    public ReservationPhase phaseAt(Instant now, Version values) {
        if (now.isBefore(checkInStart(values))) {
            return ReservationPhase.BEFORE_CHECK_IN;
        }
        if (now.isBefore(start)) {
            return ReservationPhase.CHECK_IN;
        }
        if (now.isBefore(end())) {
            return ReservationPhase.IN_JOB;
        }
        return now.isBefore(protectionWindow(values).end()) ? ReservationPhase.POST_JOB : ReservationPhase.CLOSED;
    }
}
