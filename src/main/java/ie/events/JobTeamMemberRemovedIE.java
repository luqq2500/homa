package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** A named person lapsed or was removed and came off the job's team; the customer is told. */
public record JobTeamMemberRemovedIE(String reservationId, String customerId, String providerId, String memberId, Instant occurredAt) implements ReservationIE {}
