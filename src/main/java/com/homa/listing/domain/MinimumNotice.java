package com.homa.listing.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * The provider's minimum notice (OFR-6): how long before the Check-in Period a reservation must be made (RES-4).
 * 2 hours by default; never below the acceptance policy's floor.
 */
public record MinimumNotice(Duration duration) {

    public static final MinimumNotice DEFAULT = new MinimumNotice(Duration.ofHours(2));

    public MinimumNotice {
        Objects.requireNonNull(duration, "duration");
    }

    public static MinimumNotice of(Duration duration) {
        return new MinimumNotice(duration);
    }

    /** True when this notice is at least the policy's floor: 1 hour for Instant, 2 hours for Request. */
    public boolean allowedUnder(AcceptancePolicy policy) {
        return duration.compareTo(policy.noticeFloor()) >= 0;
    }

    /** This notice, or the policy's floor when this notice is below it. */
    public MinimumNotice raisedToFloorOf(AcceptancePolicy policy) {
        return allowedUnder(policy) ? this : new MinimumNotice(policy.noticeFloor());
    }
}
