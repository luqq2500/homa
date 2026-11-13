package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** An enquiry that was never requested closed when its date passed; its chat is archived. */
public record EnquiryClosedIE(String enquiryId, Instant occurredAt) implements ReservationIE {}
