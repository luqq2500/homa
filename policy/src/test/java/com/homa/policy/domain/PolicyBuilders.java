package com.homa.policy.domain;

import java.time.Instant;

/** Test-data builders for Homa-set values. */
final class PolicyBuilders {

    static final Instant LAUNCH = Instant.parse("2026-11-01T00:00:00Z");
    static final Instant A_WEEK_LATER = Instant.parse("2026-11-08T00:00:00Z");

    private PolicyBuilders() {
    }

    /** Policy version 1, with the launch values. */
    static HomaPolicy theLaunchPolicy() {
        return HomaPolicy.initial(LAUNCH);
    }
}
