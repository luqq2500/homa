package com.homa.listing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-6")
class MinimumNoticeTest {

    private static final MinimumNotice ONE_HOUR = MinimumNotice.of(Duration.ofHours(1));
    private static final MinimumNotice TWO_HOURS = MinimumNotice.of(Duration.ofHours(2));

    @Test
    @DisplayName("The default minimum notice is 2 hours")
    void theDefaultNoticeIsTwoHours() {
        assertThat(MinimumNotice.DEFAULT).isEqualTo(TWO_HOURS);
    }

    @Test
    @DisplayName("The lowest allowed is 1 hour with the Instant policy and 2 hours with the Request policy")
    void eachPolicyHasItsOwnNoticeFloor() {
        assertThat(ONE_HOUR.allowedUnder(AcceptancePolicy.INSTANT)).isTrue();
        assertThat(ONE_HOUR.allowedUnder(AcceptancePolicy.REQUEST)).isFalse();
        assertThat(TWO_HOURS.allowedUnder(AcceptancePolicy.REQUEST)).isTrue();
        assertThat(MinimumNotice.of(Duration.ofMinutes(59)).allowedUnder(AcceptancePolicy.INSTANT)).isFalse();
        assertThat(MinimumNotice.of(Duration.ofMinutes(119)).allowedUnder(AcceptancePolicy.REQUEST)).isFalse();
    }

    @Test
    @DisplayName("A notice under the policy's floor is raised to the floor; one at or above it is kept")
    void aNoticeUnderTheFloorIsRaisedToIt() {
        MinimumNotice oneDay = MinimumNotice.of(Duration.ofHours(24));

        assertThat(ONE_HOUR.raisedToFloorOf(AcceptancePolicy.REQUEST)).isEqualTo(TWO_HOURS);
        assertThat(ONE_HOUR.raisedToFloorOf(AcceptancePolicy.INSTANT)).isSameAs(ONE_HOUR);
        assertThat(oneDay.raisedToFloorOf(AcceptancePolicy.REQUEST)).isSameAs(oneDay);
    }
}
