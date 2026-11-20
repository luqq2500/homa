package com.homa.listing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.listing.domain.exception.InvalidBookingBoundsException;
import java.time.Duration;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-5")
class BookingBoundsTest {

    private static final Duration TWO_HOURS = Duration.ofHours(2);
    private static final Duration SIX_HOURS = Duration.ofHours(6);
    private static final int ACTIVE_HELPERS = 3;

    @Test
    @DisplayName("Kemas Home sets 2 to 6 hours and leaves helpers on default. With 3 active helpers, customers can book "
            + "1 to 3 helpers. A provider that doesn't change hours can be booked for 1 to 8 hours.")
    void hoursAndHelpersStayWithinTheProvidersBounds() {
        BookingBounds kemasHome = BookingBounds.defaults().withHours(TWO_HOURS, SIX_HOURS);

        assertThat(kemasHome.allowsHours(TWO_HOURS)).isTrue();
        assertThat(kemasHome.allowsHours(SIX_HOURS)).isTrue();
        assertThat(kemasHome.allowsHours(Duration.ofMinutes(105))).isFalse();
        assertThat(kemasHome.allowsHours(Duration.ofMinutes(375))).isFalse();
        assertThat(kemasHome.maxHelpersWith(ACTIVE_HELPERS)).isEqualTo(3);
        assertThat(kemasHome.allowsHelpers(1, ACTIVE_HELPERS)).isTrue();
        assertThat(kemasHome.allowsHelpers(3, ACTIVE_HELPERS)).isTrue();
        assertThat(kemasHome.allowsHelpers(4, ACTIVE_HELPERS)).isFalse();
        assertThat(BookingBounds.defaults().allowsHours(Duration.ofHours(1))).isTrue();
        assertThat(BookingBounds.defaults().allowsHours(Duration.ofHours(8))).isTrue();
    }

    @Test
    @DisplayName("By default a customer can book 1 to 8 hours, and 1 helper up to the active team")
    void theDefaultsAreOneToEightHoursAndOneHelperUpToTheActiveTeam() {
        BookingBounds defaults = BookingBounds.defaults();

        assertThat(defaults.minHours()).isEqualTo(Duration.ofHours(1));
        assertThat(defaults.maxHours()).isEqualTo(Duration.ofHours(8));
        assertThat(defaults.minHelpers()).isEqualTo(1);
        assertThat(defaults.maxHelpers()).isEmpty();
    }

    @Test
    @DisplayName("On default bounds, 8 hours is the longest booking: 8h 15m is refused")
    void eightHoursIsTheDefaultMaximum() {
        assertThat(BookingBounds.defaults().allowsHours(Duration.ofMinutes(495))).isFalse();
    }

    @Test
    @DisplayName("Hours move in 15-minute steps: 1h 15m is allowed, 50 minutes and 1h 10m are not")
    void hoursMoveInQuarterHourSteps() {
        BookingBounds defaults = BookingBounds.defaults();

        assertThat(defaults.allowsHours(Duration.ofMinutes(75))).isTrue();
        assertThat(defaults.allowsHours(Duration.ofMinutes(50))).isFalse();
        assertThat(defaults.allowsHours(Duration.ofMinutes(70))).isFalse();
        assertThat(defaults.allowsHours(Duration.ofMinutes(75).plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("A provider's own maximum of helpers limits bookings, but never above the active team")
    void anExplicitHelperMaximumNeverExceedsTheActiveTeam() {
        BookingBounds upToTwo = BookingBounds.defaults().withHelpers(2, 2);
        BookingBounds upToFive = BookingBounds.defaults().withHelpers(1, 5);

        assertThat(upToTwo.maxHelpers()).hasValue(2);
        assertThat(upToTwo.maxHelpersWith(ACTIVE_HELPERS)).isEqualTo(2);
        assertThat(upToTwo.allowsHelpers(1, ACTIVE_HELPERS)).isFalse();
        assertThat(upToTwo.allowsHelpers(2, ACTIVE_HELPERS)).isTrue();
        assertThat(upToFive.maxHelpersWith(ACTIVE_HELPERS)).isEqualTo(3);
        assertThat(upToTwo.withHelpersUpToActiveTeam(2).maxHelpers()).isEmpty();
        assertThat(upToTwo.withHelpersUpToActiveTeam(2).minHelpers()).isEqualTo(2);
    }

    @Test
    @DisplayName("Minimum hours are at least 1 hour, both bounds are in 15-minute steps, and the maximum is not under the minimum")
    void hourBoundsMustBeInOrderAndInQuarterHourSteps() {
        BookingBounds defaults = BookingBounds.defaults();

        assertThat(defaults.withHours(Duration.ofHours(1), Duration.ofHours(1)).maxHours()).isEqualTo(Duration.ofHours(1));
        assertThat(defaults.withHours(Duration.ofHours(1), Duration.ofHours(10)).allowsHours(Duration.ofHours(10))).isTrue();
        assertThatThrownBy(() -> defaults.withHours(Duration.ofMinutes(45), SIX_HOURS))
                .isInstanceOf(InvalidBookingBoundsException.class);
        assertThatThrownBy(() -> defaults.withHours(Duration.ofMinutes(70), SIX_HOURS))
                .isInstanceOf(InvalidBookingBoundsException.class);
        assertThatThrownBy(() -> defaults.withHours(TWO_HOURS, Duration.ofMinutes(370)))
                .isInstanceOf(InvalidBookingBoundsException.class);
        assertThatThrownBy(() -> defaults.withHours(SIX_HOURS, Duration.ofMinutes(345)))
                .isInstanceOf(InvalidBookingBoundsException.class);
    }

    @Test
    @DisplayName("Minimum helpers are at least 1, and the maximum is not under the minimum")
    void helperBoundsMustBeInOrder() {
        BookingBounds defaults = BookingBounds.defaults();

        assertThat(defaults.withHelpers(1, 1).maxHelpers()).isEqualTo(OptionalInt.of(1));
        assertThatThrownBy(() -> defaults.withHelpers(0, 3)).isInstanceOf(InvalidBookingBoundsException.class);
        assertThatThrownBy(() -> defaults.withHelpersUpToActiveTeam(0)).isInstanceOf(InvalidBookingBoundsException.class);
        assertThatThrownBy(() -> defaults.withHelpers(3, 2)).isInstanceOf(InvalidBookingBoundsException.class);
    }
}
