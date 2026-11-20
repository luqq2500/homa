package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeIntervalTest {

    private static final Instant NINE_45 = Instant.parse("2026-12-05T01:45:00Z");
    private static final Instant TEN = Instant.parse("2026-12-05T02:00:00Z");
    private static final Instant TWO_PM = Instant.parse("2026-12-05T06:00:00Z");
    private static final TimeInterval JOB = new TimeInterval(TEN, TWO_PM);

    @Test
    void an_interval_must_end_after_it_starts() {
        assertThatThrownBy(() -> new TimeInterval(TEN, TEN)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(TEN.toString());
        assertThatThrownBy(() -> new TimeInterval(TWO_PM, TEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimeInterval(null, TEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TimeInterval(TEN, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void duration_of_the_standard_job_is_4_hours() {
        assertThat(JOB.duration()).isEqualTo(Duration.ofHours(4));
        assertThat(TimeInterval.of(TEN, Duration.ofHours(4))).isEqualTo(JOB);
    }

    @Test
    void RES_9_overlapping_blocks_are_detected() {
        assertThat(JOB.overlaps(new TimeInterval(TEN.plusSeconds(3600), TWO_PM.plusSeconds(3600)))).isTrue();
        assertThat(JOB.overlaps(new TimeInterval(NINE_45, TEN.plusSeconds(60)))).isTrue();
        assertThat(JOB.overlaps(new TimeInterval(TEN.plusSeconds(60), TEN.plusSeconds(120)))).isTrue();
        assertThat(JOB.overlaps(JOB)).isTrue();
    }

    @Test
    void RES_9_back_to_back_blocks_do_not_overlap() {
        assertThat(JOB.overlaps(new TimeInterval(NINE_45, TEN))).isFalse();
        assertThat(JOB.overlaps(new TimeInterval(TWO_PM, TWO_PM.plusSeconds(60)))).isFalse();
        assertThat(new TimeInterval(NINE_45, TEN).overlaps(JOB)).isFalse();
    }

    @Test
    void contains_an_instant_from_start_up_to_but_not_including_end() {
        assertThat(JOB.contains(TEN)).isTrue();
        assertThat(JOB.contains(TWO_PM.minusNanos(1))).isTrue();
        assertThat(JOB.contains(TWO_PM)).isFalse();
        assertThat(JOB.contains(TEN.minusNanos(1))).isFalse();
    }

    @Test
    void RES_3_job_time_fits_inside_working_hours_with_shared_ends() {
        TimeInterval workingHours = new TimeInterval(TEN, TWO_PM);

        assertThat(workingHours.contains(JOB)).isTrue();
        assertThat(workingHours.contains(new TimeInterval(TEN.plusSeconds(60), TWO_PM.minusSeconds(60)))).isTrue();
        assertThat(workingHours.contains(new TimeInterval(NINE_45, TWO_PM))).isFalse();
        assertThat(workingHours.contains(new TimeInterval(TEN, TWO_PM.plusSeconds(1)))).isFalse();
    }

    @Test
    void RES_9_extend_end_adds_the_buffer() {
        assertThat(JOB.extendEnd(Duration.ofMinutes(30)))
                .isEqualTo(new TimeInterval(TEN, TWO_PM.plus(Duration.ofMinutes(30))));
    }

    @Test
    void extend_start_adds_the_check_in_period() {
        assertThat(JOB.extendStart(Duration.ofMinutes(15))).isEqualTo(new TimeInterval(NINE_45, TWO_PM));
    }
}
