package com.homa.reservation.domain;

/**
 * Where a reservation stands in time, computed from its {@link JobTime} and the moment asked about, never stored
 * (plan, key decision 4). Every time-based rule asks the phase.
 */
public enum ReservationPhase {
    /** Before the Check-in Period starts: the roster can still change (ROS-1). */
    BEFORE_CHECK_IN,
    /** The 15 minutes before the job start time, when the team arrives (JOB-1). */
    CHECK_IN,
    /** From the job start time to the job end time. */
    IN_JOB,
    /** From the job end time until the Protection Window ends. */
    POST_JOB,
    /** After the Protection Window ends. */
    CLOSED
}
