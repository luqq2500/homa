package reservation.domain;

import java.time.Instant;

/** One person's face check result and its time, as the customer sees it. */
public record FaceCheck(String memberId, FaceCheckResult result, Instant checkedAt) {
}
