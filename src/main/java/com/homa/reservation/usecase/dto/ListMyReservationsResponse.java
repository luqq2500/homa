package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.List;

/** The party's reservations, earliest job first, each with its rostered members' names and verified photos (ROS-6). */
public record ListMyReservationsResponse(List<ReservationSummary> reservations) {
    public ListMyReservationsResponse {
        reservations = List.copyOf(reservations);
    }

    /** One reservation in the list. */
    public record ReservationSummary(String reservationId, String status, String customerId, String providerId,
            Instant jobStart, Instant jobEnd, int helpers, Money total, List<RosteredMember> roster) {

        public ReservationSummary {
            roster = List.copyOf(roster);
        }
    }

    /** A rostered member, with the name and verified photo the customer sees (ROS-6). */
    public record RosteredMember(String memberId, String role, String name, String photoRef) {
    }
}
