package com.homa.reservation.usecase.dto;

import java.util.List;

/**
 * What this run did. A request another run, or a provider or customer, got to first is skipped: whoever saved it
 * first decided it.
 *
 * @param expiredReservationIds the reservations this run expired
 * @param skippedReservationIds the overdue reservations that were no longer Requested, or changed under this run
 */
public record ExpireOverdueRequestsResponse(List<String> expiredReservationIds, List<String> skippedReservationIds) {

    public ExpireOverdueRequestsResponse {
        expiredReservationIds = List.copyOf(expiredReservationIds);
        skippedReservationIds = List.copyOf(skippedReservationIds);
    }
}
