package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** A customer started an enquiry; its chat opens. */
public record EnquiryStartedIE(String enquiryId, String customerId, String providerId, Instant occurredAt) implements ReservationIE {}
