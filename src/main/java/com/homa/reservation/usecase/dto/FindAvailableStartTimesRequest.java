package com.homa.reservation.usecase.dto;

import java.time.LocalDate;
import java.util.List;

/** The job start times the customer can choose on a date for these jobs, hours and helpers (RES-3 to RES-5, RES-9).
 *
 * @param minutes the job's length in minutes */
public record FindAvailableStartTimesRequest(String customerId, String providerId, LocalDate date, List<String> jobCodes, int minutes, int helpers) {
    public FindAvailableStartTimesRequest {
        jobCodes = List.copyOf(jobCodes);
    }
}
