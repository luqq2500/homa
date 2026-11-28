package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.ExpireOverdueRequestsRequest;
import com.homa.reservation.usecase.dto.ExpireOverdueRequestsResponse;
import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.in.OverdueRequestExpirer;
import com.homa.reservation.usecase.port.in.RequestExpirer;
import com.homa.reservation.usecase.port.out.ReservationChangedException;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * REQ-4, run by a scheduler every minute: every Requested reservation whose acceptance deadline has been reached
 * expires, each through {@link RequestExpirer} so each is its own unit of work. The run is safe to repeat and to run
 * in several tasks at once: a reservation that is no longer Requested, or that another run changed first, is skipped,
 * and it is expired and published exactly once, by whoever saves it first. Any other failure ends this run and the
 * next minute's run picks the rest up.
 */
public final class ExpireOverdueRequests implements OverdueRequestExpirer {

    private final ReservationRepository reservations;
    private final RequestExpirer expirer;
    private final TimeSource time;

    public ExpireOverdueRequests(ReservationRepository reservations, RequestExpirer expirer, TimeSource time) {
        this.reservations = reservations;
        this.expirer = expirer;
        this.time = time;
    }

    @Override
    public ExpireOverdueRequestsResponse expireOverdue(ExpireOverdueRequestsRequest request) {
        List<String> expired = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Reservation overdue : reservations.findRequestedPastDeadline(time.now())) {
            String id = overdue.id().value();
            try {
                expirer.expire(new ExpireRequestRequest(id));
                expired.add(id);
            } catch (ReservationStatusConflictException | ReservationChangedException
                    | ScheduleChangedException decidedElsewhere) {
                skipped.add(id);
            }
        }
        return new ExpireOverdueRequestsResponse(expired, skipped);
    }
}
