package com.homa.availability.domain;

import static com.homa.availability.domain.AvailabilityBuilders.busy;
import static com.homa.availability.domain.AvailabilityBuilders.saturday;
import static com.homa.availability.domain.AvailabilityBuilders.slot;
import static com.homa.availability.domain.AvailabilityBuilders.window;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.availability.domain.exception.SlotStartNotOnQuarterHourException;
import com.homa.availability.domain.exception.TimeRangeEndsBeforeItStartsException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Slots, windows and busy blocks. */
class TimeRangeTest {

    @ParameterizedTest(name = "a slot can start at {0}")
    @ValueSource(strings = {"10:00", "10:15", "10:30", "10:45"})
    void slotsStartOnQuarterHours(String start) {
        assertThat(TimeSlot.of(saturday(start), Duration.ofHours(3)).length()).isEqualTo(Duration.ofHours(3));
    }

    @ParameterizedTest(name = "a slot cannot start at {0}")
    @ValueSource(strings = {"10:10", "10:00:01", "10:14"})
    void slotsCannotStartBetweenQuarterHours(String start) {
        assertThatThrownBy(() -> TimeSlot.of(saturday(start), Duration.ofHours(3)))
                .isInstanceOf(SlotStartNotOnQuarterHourException.class);
    }

    @Test
    void aSlotCannotStartBetweenSeconds() {
        assertThatThrownBy(() -> TimeSlot.of(saturday("10:00").plusNanos(1), Duration.ofHours(3)))
                .isInstanceOf(SlotStartNotOnQuarterHourException.class);
    }

    @Test
    void everyRangeEndsAfterItStarts() {
        assertThatThrownBy(() -> slot("13:00", "10:00")).isInstanceOf(TimeRangeEndsBeforeItStartsException.class);
        assertThatThrownBy(() -> slot("10:00", "10:00")).isInstanceOf(TimeRangeEndsBeforeItStartsException.class);
        assertThatThrownBy(() -> window("18:00", "08:00")).isInstanceOf(TimeRangeEndsBeforeItStartsException.class);
        assertThatThrownBy(() -> busy("12:00", "12:00")).isInstanceOf(TimeRangeEndsBeforeItStartsException.class);
    }

    @Test
    void rangesOverlapOnlyWhenTheyShareTime() {
        TimeSlot booking = slot("10:00", "13:00");

        assertThat(booking.overlaps(busy("12:59", "14:00"))).isTrue();
        assertThat(booking.overlaps(busy("09:00", "10:01"))).isTrue();
        assertThat(booking.overlaps(busy("11:00", "12:00"))).isTrue();
        assertThat(booking.overlaps(busy("09:00", "14:00"))).isTrue();
        assertThat(booking.overlaps(busy("13:00", "14:00"))).isFalse();
        assertThat(booking.overlaps(busy("09:00", "10:00"))).isFalse();
    }

    @Test
    void aRangeIncludesItsStartButNotItsEnd() {
        AvailabilityWindow morning = window("08:00", "12:00");

        assertThat(morning.includes(saturday("08:00"))).isTrue();
        assertThat(morning.includes(saturday("11:59"))).isTrue();
        assertThat(morning.includes(saturday("12:00"))).isFalse();
        assertThat(morning.includes(saturday("07:59"))).isFalse();
    }
}
