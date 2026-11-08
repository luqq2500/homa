package availability.domain;

import java.time.Duration;
import java.util.Objects;

/** The slot an accepted reservation holds on the provider's calendar. */
public record ReservationHold(String reservationId, TimeSlot slot) {

    public ReservationHold {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(slot);
    }

    /** Whether this hold, with {@code buffer} kept free on each side, leaves no room for {@code wanted}. */
    boolean blocks(TimeSlot wanted, Duration buffer) {
        return wanted.start().isBefore(slot.end().plus(buffer)) && slot.start().minus(buffer).isBefore(wanted.end());
    }
}
