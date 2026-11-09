package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A named person took a face check at check-in; {@code result} is PASSED or FAILED. Homa keeps only the result and time, never the selfie. */
public record FaceCheckRecordedIE(
        String reservationId,
        String memberId,
        String result,
        Instant occurredAt) implements ReservationIE {

    public FaceCheckRecordedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
