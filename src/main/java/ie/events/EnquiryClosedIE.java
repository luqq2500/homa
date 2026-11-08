package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** An enquiry's date passed without a request; its chat is archived. */
public record EnquiryClosedIE(
        String enquiryId,
        Instant occurredAt) implements ReservationIE {

    public EnquiryClosedIE {
        Objects.requireNonNull(enquiryId, "enquiryId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
