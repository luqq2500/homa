package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** A named person failed the face check and cannot work on this job; the customer is told. */
public record FaceCheckFailedIE(String reservationId, String customerId, String providerId, String memberId, Instant occurredAt) implements ReservationIE {}
