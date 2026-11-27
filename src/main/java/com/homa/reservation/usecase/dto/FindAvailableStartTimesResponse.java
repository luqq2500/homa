package com.homa.reservation.usecase.dto;

import java.time.Instant;
import java.util.List;

/** The free job start times, earliest first. A time shown free is not held (RES-11). */
public record FindAvailableStartTimesResponse(List<Instant> startTimes) {
    public FindAvailableStartTimesResponse {
        startTimes = List.copyOf(startTimes);
    }
}
