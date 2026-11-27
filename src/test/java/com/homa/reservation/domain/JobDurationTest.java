package com.homa.reservation.domain;

import static com.homa.reservation.domain.ReservationFixtures.BOOKING_STEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.exception.InvalidJobDurationException;
import com.homa.reservation.domain.replica.BookingBounds;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class JobDurationTest {

    /** A provider on Listing's defaults: 1 to 8 hours in 15-minute steps. */
    private static final BookingBounds DEFAULT_BOUNDS =
            new BookingBounds(Duration.ofHours(1), Duration.ofHours(8), BOOKING_STEP, 1, 3);
    /** Kemas Home: 2 to 6 hours (OFR-5). */
    private static final BookingBounds KEMAS_BOUNDS =
            new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), BOOKING_STEP, 1, 3);

    @Test
    @Tag("RES-5")
    @DisplayName("Aisyah can book 1h 15m but not 1h 10m. With Kemas Home's 2-hour minimum, she starts at 2 hours.")
    void jobLengthMatchesTheProvidersBoundsAndSteps() {
        assertThat(JobDuration.ofMinutes(60).fits(DEFAULT_BOUNDS)).isTrue();
        assertThat(JobDuration.ofMinutes(75).fits(DEFAULT_BOUNDS)).isTrue();
        assertThat(JobDuration.ofMinutes(90).fits(DEFAULT_BOUNDS)).isTrue();
        assertThat(JobDuration.ofMinutes(50).fits(DEFAULT_BOUNDS)).isFalse();
        assertThat(JobDuration.ofMinutes(70).fits(DEFAULT_BOUNDS)).isFalse();

        assertThat(JobDuration.ofMinutes(105).fits(KEMAS_BOUNDS)).isFalse();
        assertThat(JobDuration.ofMinutes(120).fits(KEMAS_BOUNDS)).isTrue();
    }

    @Test
    @Tag("RES-5")
    @DisplayName("The maximum itself can be booked, one step more can't; 1 hour and 1 second is not a whole step.")
    void jobLengthIsRefusedJustPastTheMaximumAndJustOffAStep() {
        assertThat(JobDuration.ofMinutes(480).fits(DEFAULT_BOUNDS)).isTrue();
        assertThat(JobDuration.ofMinutes(495).fits(DEFAULT_BOUNDS)).isFalse();
        assertThat(JobDuration.of(Duration.ofHours(1).plusSeconds(1)).fits(DEFAULT_BOUNDS)).isFalse();
    }

    @Test
    @Tag("RES-5")
    @DisplayName("A job must last some time, and Listing's step must be a real step.")
    void jobMustLastSomeTimeAndTheStepMustBePositive() {
        assertThat(JobDuration.ofMinutes(1).length()).isEqualTo(Duration.ofMinutes(1));
        assertThatThrownBy(() -> JobDuration.ofMinutes(0)).isInstanceOf(InvalidJobDurationException.class);
        assertThatThrownBy(() -> JobDuration.ofMinutes(-15)).isInstanceOf(InvalidJobDurationException.class);
        assertThatThrownBy(() -> new BookingBounds(Duration.ofHours(1), Duration.ofHours(8), Duration.ZERO, 1, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new BookingBounds(Duration.ofHours(1), Duration.ofHours(8), Duration.ofNanos(1), 1, 3).step())
                .isEqualTo(Duration.ofNanos(1));
    }

    @Test
    @Tag("RES-6")
    @DisplayName("Hours are priced exactly: 1h 15m is 1.25 hours and 4 hours is 4.")
    void jobLengthInHoursIsExact() {
        assertThat(JobDuration.ofMinutes(75).inHours()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(JobDuration.ofMinutes(240).inHours()).isEqualByComparingTo(new BigDecimal("4"));
    }
}
