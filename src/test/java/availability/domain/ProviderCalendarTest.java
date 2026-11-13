package availability.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import availability.domain.exception.InvalidBufferException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kernel.TimeRange;
import org.junit.jupiter.api.Test;

class ProviderCalendarTest {

    private static TimeRange hours(int from, int to) {
        Instant day = Instant.parse("2026-03-07T00:00:00Z");
        return TimeRange.of(day.plus(Duration.ofHours(from)), day.plus(Duration.ofHours(to)));
    }

    private final ProviderCalendar calendar = ProviderCalendar.startFor("provider-1");

    @Test
    void aNewCalendarHasNoOpenTimeAndNoBuffer() {
        assertThat(calendar.providerId()).isEqualTo("provider-1");
        assertThat(calendar.slots()).isEmpty();
        assertThat(calendar.events()).isEmpty();
        assertThat(calendar.buffer()).isEqualTo(Duration.ZERO);
        assertThat(calendar.openTime()).isEmpty();
    }

    @Test
    void availabilityTimesThatRunBackToBackCountAsOneStretch() {
        calendar.setAvailability(List.of(hours(12, 14), hours(9, 12), hours(16, 18)));

        assertThat(calendar.slots()).containsExactly(hours(9, 14), hours(16, 18));
        assertThat(calendar.openTime()).containsExactly(hours(9, 14), hours(16, 18));
    }

    @Test
    void settingAvailabilityAgainReplacesIt() {
        calendar.setAvailability(List.of(hours(9, 12)));
        calendar.setAvailability(List.of(hours(14, 16)));

        assertThat(calendar.openTime()).containsExactly(hours(14, 16));
    }

    @Test
    void syncedCalendarEventsAreTakenOutOfTheOpenTime() {
        calendar.setAvailability(List.of(hours(9, 18)));

        calendar.syncEvents(List.of(hours(12, 13), hours(15, 16), hours(15, 17)));

        assertThat(calendar.events()).containsExactly(hours(12, 13), hours(15, 17));
        assertThat(calendar.openTime()).containsExactly(hours(9, 12), hours(13, 15), hours(17, 18));
        assertThat(calendar.slots()).containsExactly(hours(9, 18));
    }

    @Test
    void anEventThatEndsExactlyWhenASlotStartsDoesNotBlockIt() {
        calendar.setAvailability(List.of(hours(10, 12)));

        calendar.syncEvents(List.of(hours(8, 10), hours(12, 13)));

        assertThat(calendar.openTime()).containsExactly(hours(10, 12));
    }

    @Test
    void aNewSyncReplacesTheEarlierEvents() {
        calendar.setAvailability(List.of(hours(9, 12)));
        calendar.syncEvents(List.of(hours(9, 10)));

        calendar.syncEvents(List.of());

        assertThat(calendar.openTime()).containsExactly(hours(9, 12));
    }

    @Test
    void theBufferBetweenJobsIsZeroOrMore() {
        calendar.setBuffer(Duration.ofMinutes(30));
        assertThat(calendar.buffer()).isEqualTo(Duration.ofMinutes(30));
        calendar.setBuffer(Duration.ZERO);
        assertThat(calendar.buffer()).isEqualTo(Duration.ZERO);

        assertThatThrownBy(() -> calendar.setBuffer(Duration.ofMinutes(-1))).isInstanceOf(InvalidBufferException.class);
        assertThatThrownBy(() -> calendar.setBuffer(null)).isInstanceOf(InvalidBufferException.class);
        assertThat(calendar.buffer()).isEqualTo(Duration.ZERO);
    }

    @Test
    void theBufferDoesNotChangeTheOpenTime() {
        calendar.setAvailability(List.of(hours(9, 12)));

        calendar.setBuffer(Duration.ofHours(1));

        assertThat(calendar.openTime()).containsExactly(hours(9, 12));
    }

    @Test
    void aStoredCalendarIsCheckedAndMergedLikeANewOne() {
        ProviderCalendar stored = new ProviderCalendar("provider-1", List.of(hours(9, 10), hours(10, 11)),
                List.of(hours(9, 10)), Duration.ofMinutes(15));

        assertThat(stored.slots()).containsExactly(hours(9, 11));
        assertThat(stored.openTime()).containsExactly(hours(10, 11));
        assertThat(stored.buffer()).isEqualTo(Duration.ofMinutes(15));
        assertThatThrownBy(() -> new ProviderCalendar("provider-1", List.of(), List.of(), Duration.ofMinutes(-5)))
                .isInstanceOf(InvalidBufferException.class);
    }
}
