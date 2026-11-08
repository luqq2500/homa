package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** An enquiry turned into a reservation; its chat carries on as the reservation's chat. */
public record EnquiryRequestedIE(
        String enquiryId,
        String reservationId,
        Instant occurredAt) implements ReservationIE {

    public EnquiryRequestedIE {
        Objects.requireNonNull(enquiryId, "enquiryId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
