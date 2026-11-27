package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.List;

/** A reservation as its customer and provider see it, with each rostered member's name and verified photo (ROS-6).
 *
 * @param acceptanceDeadline null under Instant, which never waits for acceptance (ACC-3)
 * @param reservedAt         null until Reserved
 * @param completedAt        null until Completed */
public record ViewReservationResponse(String reservationId, String status, String customerId, String providerId, List<String> jobCodes,
        Instant checkInStart, Instant jobStart, Instant jobEnd, int helpers, String addressLine, String city,
        String arrivalNote, Money hoursFee, Money travelFee, Money total, Instant requestedAt,
        Instant acceptanceDeadline, Instant reservedAt, Instant completedAt, List<RosteredMember> roster,
        List<String> completionPhotoRefs) {
    public ViewReservationResponse {
        jobCodes = List.copyOf(jobCodes);
        roster = List.copyOf(roster);
        completionPhotoRefs = List.copyOf(completionPhotoRefs);
    }

    /** A rostered member, with the name and verified photo the customer sees (ROS-6). */
    public record RosteredMember(String memberId, String role, String name, String photoRef) {
    }
}
