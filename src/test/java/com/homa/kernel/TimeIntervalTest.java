package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TimeIntervalTest {

    private static final Instant NINE_45 = Instant.parse("2026-12-05T01:45:00Z");
    private static final Instant TEN = Instant.parse("2026-12-05T02:00:00Z");
    private static final Instant TWO_PM = Instant.parse("2026-12-05T06:00:00Z");
    private static final TimeInterval JOB = new TimeInterval(TEN, TWO_PM);

    @Test
    @DisplayName("A period ends after it starts.")
    void intervalMustEndAfterItStarts() {
        assertThatThrownBy(() -> new TimeInterval(TEN, TEN)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(TEN.toString());
        assertThatThrownBy(() -> new TimeInterval(TWO_PM, TEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimeInterval(null, TEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TimeInterval(TEN, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Saturday 10:00–14:00 is 4 hours.")
    void standardJobTimeLastsFourHours() {
        assertThat(JOB.duration()).isEqualTo(Duration.ofHours(4));
        assertThat(TimeInterval.of(TEN, Duration.ofHours(4))).isEqualTo(JOB);
    }

    @Test
    @Tag("RES-9")
    @DisplayName("A provider's blocked times cannot overlap.")
    void overlappingBlocksAreDetected() {
        assertThat(JOB.overlaps(new TimeInterval(TEN.plusSeconds(3600), TWO_PM.plusSeconds(3600)))).isTrue();
        assertThat(JOB.overlaps(new TimeInterval(NINE_45, TEN.plusSeconds(60)))).isTrue();
        assertThat(JOB.overlaps(new TimeInterval(TEN.plusSeconds(60), TEN.plusSeconds(120)))).isTrue();
        assertThat(JOB.overlaps(JOB)).isTrue();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("A block ending at 10:00 and one starting at 10:00 do not overlap.")
    void backToBackBlocksDoNotOverlap() {
        assertThat(JOB.overlaps(new TimeInterval(NINE_45, TEN))).isFalse();
        assertThat(JOB.overlaps(new TimeInterval(TWO_PM, TWO_PM.plusSeconds(60)))).isFalse();
        assertThat(new TimeInterval(NINE_45, TEN).overlaps(JOB)).isFalse();
    }

    @Test
    @DisplayName("10:00 is inside 10:00–14:00; 14:00 is not.")
    void intervalContainsItsStartButNotItsEnd() {
        assertThat(JOB.contains(TEN)).isTrue();
        assertThat(JOB.contains(TWO_PM.minusNanos(1))).isTrue();
        assertThat(JOB.contains(TWO_PM)).isFalse();
        assertThat(JOB.contains(TEN.minusNanos(1))).isFalse();
    }

    @Test
    @Tag("RES-3")
    @DisplayName("The job time must fit inside working hours; it may start and end exactly at their edges.")
    void jobTimeFitsInsideWorkingHoursWithSharedEnds() {
        TimeInterval workingHours = new TimeInterval(TEN, TWO_PM);

        assertThat(workingHours.contains(JOB)).isTrue();
        assertThat(workingHours.contains(new TimeInterval(TEN.plusSeconds(60), TWO_PM.minusSeconds(60)))).isTrue();
        assertThat(workingHours.contains(new TimeInterval(NINE_45, TWO_PM))).isFalse();
        assertThat(workingHours.contains(new TimeInterval(TEN, TWO_PM.plusSeconds(1)))).isFalse();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("The provider's block runs to the job end plus the buffer.")
    void extendingTheEndAddsTheBuffer() {
        assertThat(JOB.extendEnd(Duration.ofMinutes(30)))
                .isEqualTo(new TimeInterval(TEN, TWO_PM.plus(Duration.ofMinutes(30))));
    }

    @Test
    @DisplayName("The job time plus its Check-in Period starts at 9:45.")
    void extendingTheStartAddsTheCheckInPeriod() {
        assertThat(JOB.extendStart(Duration.ofMinutes(15))).isEqualTo(new TimeInterval(NINE_45, TWO_PM));
    }
}
