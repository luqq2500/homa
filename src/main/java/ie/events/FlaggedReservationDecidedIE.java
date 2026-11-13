package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;

/** An admin decided a reservation disputed for a serious flag: cancelled with a full refund, or restored. */
public record FlaggedReservationDecidedIE(String reservationId, boolean restored, String adminId, String reason, Instant occurredAt) implements ResolutionIE {}
