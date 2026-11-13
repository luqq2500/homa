package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** The provider reported a customer no-show; the customer can report a wrongful one until the window closes. */
public record CustomerNoShowReportedIE(String reservationId, String customerId, String providerId, Instant reportWindowClosesAt, Instant occurredAt) implements ReservationIE {}
