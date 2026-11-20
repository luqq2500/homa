package com.homa.listing.domain;

import java.time.Duration;

/**
 * The provider's Reservation Acceptance Policy (§4.3, OFR-9), with the lowest minimum notice it allows (OFR-6).
 * Request needs 2 hours, so a Request provider always has at least 1 hour to respond (ACC-3).
 */
public enum AcceptancePolicy {
    INSTANT(Duration.ofHours(1)),
    REQUEST(Duration.ofHours(2));

    private final Duration noticeFloor;

    AcceptancePolicy(Duration noticeFloor) {
        this.noticeFloor = noticeFloor;
    }

    public Duration noticeFloor() {
        return noticeFloor;
    }
}
