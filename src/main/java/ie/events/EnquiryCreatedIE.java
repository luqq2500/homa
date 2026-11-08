package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A customer created an enquiry, directly or by booking again, so they can chat first. */
public record EnquiryCreatedIE(
        String enquiryId,
        String customerId,
        String providerId,
        Instant start,
        Instant occurredAt) implements ReservationIE {

    public EnquiryCreatedIE {
        Objects.requireNonNull(enquiryId, "enquiryId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
