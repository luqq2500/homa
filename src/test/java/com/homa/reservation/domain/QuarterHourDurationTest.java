package com.homa.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.exception.InvalidJobDurationException;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class QuarterHourDurationTest {

    @Test
    @Tag("RES-5")
    @DisplayName("With a provider whose minimum is 1 hour, Aisyah can book 1h, 1h 15m, 1h 30m and so on, but not 50 minutes or 1h 10m.")
    void jobDurationIsAtLeastOneHourInQuarterHourSteps() {
        assertThat(QuarterHourDuration.ofMinutes(60).length()).isEqualTo(Duration.ofHours(1));
        assertThat(QuarterHourDuration.ofMinutes(75).length()).isEqualTo(Duration.ofMinutes(75));
        assertThat(QuarterHourDuration.ofMinutes(90).length()).isEqualTo(Duration.ofMinutes(90));

        assertThatThrownBy(() -> QuarterHourDuration.ofMinutes(50)).isInstanceOf(InvalidJobDurationException.class);
        assertThatThrownBy(() -> QuarterHourDuration.ofMinutes(70)).isInstanceOf(InvalidJobDurationException.class);
    }

    @Test
    @Tag("RES-5")
    @DisplayName("45 minutes is a whole step but under the 1-hour minimum; 1 hour and 1 second is not a whole step.")
    void jobDurationIsRefusedJustUnderTheMinimumAndJustOffAStep() {
        assertThatThrownBy(() -> QuarterHourDuration.ofMinutes(45)).isInstanceOf(InvalidJobDurationException.class);
        assertThatThrownBy(() -> QuarterHourDuration.of(Duration.ofHours(1).plusSeconds(1)))
                .isInstanceOf(InvalidJobDurationException.class);
    }

    @Test
    @Tag("RES-6")
    @DisplayName("Hours are priced exactly: 1h 15m is 1.25 hours and 4 hours is 4.")
    void jobDurationInHoursIsExact() {
        assertThat(QuarterHourDuration.ofMinutes(75).inHours()).isEqualByComparingTo(new BigDecimal("1.25"));
        assertThat(QuarterHourDuration.ofMinutes(240).inHours()).isEqualByComparingTo(new BigDecimal("4"));
    }
}
