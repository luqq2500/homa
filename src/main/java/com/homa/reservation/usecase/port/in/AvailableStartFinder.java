package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.FindAvailableStartTimesRequest;
import com.homa.reservation.usecase.dto.FindAvailableStartTimesResponse;

/** RES-3, RES-4, RES-5, RES-9: the job start times a customer can choose on a date. */
public interface AvailableStartFinder {

    FindAvailableStartTimesResponse find(FindAvailableStartTimesRequest request);
}
