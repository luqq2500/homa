package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** The provider checked in at the door. */
public record ProviderCheckedInIE(String reservationId, String customerId, String providerId, Instant checkedInAt, boolean faceCheckUnavailable, Instant occurredAt) implements ReservationIE {}
