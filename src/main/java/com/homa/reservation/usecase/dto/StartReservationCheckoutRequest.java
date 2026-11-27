package com.homa.reservation.usecase.dto;

import java.time.Instant;
import java.util.List;

/** The customer starts paying for a booking at their home address (RES-2).
 *
 * @param minutes the job's length in minutes */
public record StartReservationCheckoutRequest(String customerId, String providerId, List<String> jobCodes, Instant jobStart, int minutes, int helpers) {
    public StartReservationCheckoutRequest {
        jobCodes = List.copyOf(jobCodes);
    }
}
