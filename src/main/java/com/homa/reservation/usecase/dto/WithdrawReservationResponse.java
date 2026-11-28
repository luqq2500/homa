package com.homa.reservation.usecase.dto;

import java.time.Instant;

/**
 * The outcome of a withdrawal (REQ-2, REQ-5). {@code outcome} is {@link #WITHDRAWN}, or {@link #ALREADY_ACCEPTED}
 * when the provider's acceptance was recorded first: the reservation is Reserved, nothing was withdrawn, and the app
 * offers to cancel it instead, free within the Grace Period (CCX-1).
 *
 * @param status      the reservation's status after the call: WITHDRAWN, or RESERVED when already accepted
 * @param withdrawnAt null unless withdrawn
 */
public record WithdrawReservationResponse(String reservationId, String outcome, String status, Instant withdrawnAt) {

    public static final String WITHDRAWN = "WITHDRAWN";
    public static final String ALREADY_ACCEPTED = "ALREADY_ACCEPTED";
}
