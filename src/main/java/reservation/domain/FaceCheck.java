package reservation.domain;

import java.time.Instant;
import java.util.Objects;

/** One face check attempt by a named person, with its result and time, as the customer sees it. Homa keeps no selfie. */
public record FaceCheck(String memberId, FaceCheckResult result, Instant at) {

    public FaceCheck {
        memberId = ReservationText.require(memberId, "A team member id");
        Objects.requireNonNull(result);
        Objects.requireNonNull(at);
    }

    boolean passed() {
        return result == FaceCheckResult.PASSED;
    }
}
