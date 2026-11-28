package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.LockRosterRequest;
import com.homa.reservation.usecase.dto.LockRosterResponse;

/**
 * ROS-1, ROS-7, NRS-1 to NRS-3: one reservation's roster locks at its Check-in Period, or the reservation is
 * cancelled for having no complete roster. This is the unit of work of the scheduler's sweep
 * ({@link CheckInRosterLocker}): each call is its own transaction, so one reservation's conflict never undoes
 * another's lock.
 */
public interface RosterLocker {

    LockRosterResponse lock(LockRosterRequest request);
}
