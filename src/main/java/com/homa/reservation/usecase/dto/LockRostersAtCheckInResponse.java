package com.homa.reservation.usecase.dto;

import java.util.List;

/**
 * What this run did. A reservation another run, or the customer or the provider, got to first is skipped: whoever
 * saved it first decided it.
 *
 * @param lockedReservationIds    the reservations whose complete roster this run locked (ROS-1)
 * @param cancelledReservationIds the reservations this run cancelled for having no complete roster (NRS-1)
 * @param skippedReservationIds   the reservations that no longer needed locking, or changed under this run
 */
public record LockRostersAtCheckInResponse(
        List<String> lockedReservationIds, List<String> cancelledReservationIds, List<String> skippedReservationIds) {

    public LockRostersAtCheckInResponse {
        lockedReservationIds = List.copyOf(lockedReservationIds);
        cancelledReservationIds = List.copyOf(cancelledReservationIds);
        skippedReservationIds = List.copyOf(skippedReservationIds);
    }
}
