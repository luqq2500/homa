package com.homa.reservation.domain;

import com.homa.kernel.TimeInterval;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The booked period from the job start time to the job end time (§1 "Job time"). It answers every time question
 * about a reservation, so no use case does date arithmetic.
 */
public record JobTime(Instant start, QuarterHourDuration duration) {

    /** §1 "Check-in Period": the 15 minutes before the job start time. */
    static final Duration CHECK_IN_LENGTH = Duration.ofMinutes(15);
    /** §1 "Protection Window": until 24 hours after the job end time. */
    static final Duration PROTECTION_AFTER_JOB = Duration.ofHours(24);
    /** JOB-5: the second completion reminder comes 2 hours after the job end time. */
    static final Duration SECOND_COMPLETION_REMINDER = Duration.ofHours(2);

    public JobTime {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(duration, "duration");
    }

    public static JobTime of(Instant start, Duration length) {
        return new JobTime(start, QuarterHourDuration.of(length));
    }

    public Instant end() {
        return start.plus(duration.length());
    }

    /** The job time itself, [start, end): what the customer's calendar blocks (RES-10). */
    public TimeInterval interval() {
        return new TimeInterval(start, end());
    }

    /** [start − 15 min, start): the team arrives in this period (JOB-1). */
    public TimeInterval checkInPeriod() {
        return new TimeInterval(checkInStart(), start);
    }

    /** [Check-in Period start, job end + 24 h). */
    public TimeInterval protectionWindow() {
        return new TimeInterval(checkInStart(), end().plus(PROTECTION_AFTER_JOB));
    }

    /** §1 "Notice": from now until the Check-in Period starts (RES-4). Negative once it has started. */
    public Duration noticeFrom(Instant now) {
        return Duration.between(now, checkInStart());
    }

    /** [Check-in Period start, job end + buffer): what the provider's calendar blocks (RES-9). */
    public TimeInterval calendarBlock(Duration buffer) {
        return new TimeInterval(checkInStart(), end().plus(buffer));
    }

    /** JOB-5: the provider is reminded at the job end time and 2 hours later while the job isn't marked Completed. */
    public List<Instant> completionReminders() {
        return List.of(end(), end().plus(SECOND_COMPLETION_REMINDER));
    }

    public ReservationPhase phaseAt(Instant now) {
        if (now.isBefore(checkInStart())) {
            return ReservationPhase.BEFORE_CHECK_IN;
        }
        if (now.isBefore(start)) {
            return ReservationPhase.CHECK_IN;
        }
        if (now.isBefore(end())) {
            return ReservationPhase.IN_JOB;
        }
        return now.isBefore(protectionWindow().end()) ? ReservationPhase.POST_JOB : ReservationPhase.CLOSED;
    }

    private Instant checkInStart() {
        return start.minus(CHECK_IN_LENGTH);
    }
}
