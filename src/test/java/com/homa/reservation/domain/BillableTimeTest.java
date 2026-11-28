package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BillableTimeTest {

    private static final Duration QUARTER_HOUR = Duration.ofMinutes(15);

    private static BillableTime afterFirstHour(Duration worked) {
        return BillableTime.afterFirstHour(JOB_START, JOB_START.plus(worked), QUARTER_HOUR);
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("Aisyah cancels at 9:50: she pays 45 + 0 + 20 = RM65 and gets RM135 back.")
    void nothingIsChargedAfterTheFirstHourBeforeTheJobStartsOrInTheFirstHour() {
        assertThat(BillableTime.afterFirstHour(JOB_START, JOB_START.minus(Duration.ofMinutes(10)), QUARTER_HOUR)
                .duration()).isEqualTo(Duration.ZERO);
        assertThat(afterFirstHour(Duration.ZERO).duration()).isEqualTo(Duration.ZERO);
        assertThat(afterFirstHour(Duration.ofMinutes(59)).duration()).isEqualTo(Duration.ZERO);
        assertThat(afterFirstHour(Duration.ofHours(1)).duration()).isEqualTo(Duration.ZERO);
        assertThat(afterFirstHour(Duration.ZERO).inHours()).isEqualByComparingTo("0");
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Hours worked after the first hour are charged in 15-minute steps, rounded up.")
    void everyStartedQuarterHourAfterTheFirstHourIsCharged() {
        assertThat(afterFirstHour(Duration.ofHours(1).plusSeconds(1)).duration()).isEqualTo(QUARTER_HOUR);
        assertThat(afterFirstHour(Duration.ofMinutes(75)).duration()).isEqualTo(QUARTER_HOUR);
        assertThat(afterFirstHour(Duration.ofMinutes(75).plusSeconds(1)).duration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(afterFirstHour(Duration.ofMinutes(80)).duration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(afterFirstHour(Duration.ofHours(3)).duration()).isEqualTo(Duration.ofHours(2));
        assertThat(afterFirstHour(Duration.ofMinutes(239)).duration()).isEqualTo(Duration.ofHours(3));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Aisyah stops the job at 11:20: 30 minutes are charged (RM22.50), so she pays RM87.50 and gets RM112.50 back.")
    void chargedTimeIsCountedInExactHours() {
        assertThat(afterFirstHour(Duration.ofMinutes(80)).inHours()).isEqualByComparingTo("0.5");
        assertThat(afterFirstHour(Duration.ofMinutes(70)).inHours()).isEqualByComparingTo("0.25");
        assertThat(afterFirstHour(Duration.ofMinutes(180)).inHours()).isEqualByComparingTo("2");
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("The step is Homa's partial-time step: with 30-minute steps, one started minute after the first hour charges 30 minutes.")
    void theStepIsTheOneGiven() {
        BillableTime halfHourSteps = BillableTime.afterFirstHour(JOB_START,
                JOB_START.plus(Duration.ofMinutes(61)), Duration.ofMinutes(30));

        assertThat(halfHourSteps.duration()).isEqualTo(Duration.ofMinutes(30));
        assertThatThrownBy(() -> new BillableTime(null)).isInstanceOf(NullPointerException.class);
    }
}
