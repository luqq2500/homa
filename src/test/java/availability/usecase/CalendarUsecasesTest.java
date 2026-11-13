package availability.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import availability.domain.exception.InvalidBufferException;
import availability.support.InMemoryProviderCalendarRepository;
import availability.support.RecordingAvailabilityIEPublisher;
import availability.usecase.dto.SetAvailabilityRequest;
import availability.usecase.dto.SetBufferRequest;
import availability.usecase.dto.SyncCalendarEventsRequest;
import availability.usecase.dto.ViewCalendarRequest;
import availability.usecase.dto.ViewCalendarResponse;
import ie.events.ProviderCalendarChangedIE;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import kernel.TimeRange;
import org.junit.jupiter.api.Test;

class CalendarUsecasesTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    private static final String PROVIDER = "provider-1";

    private final InMemoryProviderCalendarRepository calendars = new InMemoryProviderCalendarRepository();
    private final RecordingAvailabilityIEPublisher publisher = new RecordingAvailabilityIEPublisher();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ProviderSetAvailability availability = new ProviderSetAvailability(calendars, publisher, clock);
    private final ProviderSetBuffer buffer = new ProviderSetBuffer(calendars, publisher, clock);
    private final CalendarSyncEvents sync = new CalendarSyncEvents(calendars, publisher, clock);

    private static TimeRange hours(int from, int to) {
        Instant day = Instant.parse("2026-03-08T00:00:00Z");
        return TimeRange.of(day.plus(Duration.ofHours(from)), day.plus(Duration.ofHours(to)));
    }

    @Test
    void settingAvailabilityStartsTheCalendarAndAnnouncesTheOpenTime() {
        List<TimeRange> open = availability.set(new SetAvailabilityRequest(PROVIDER, List.of(hours(9, 12), hours(12, 14))))
                .openTime();

        assertThat(open).containsExactly(hours(9, 14));
        assertThat(calendars.timesSaved(PROVIDER)).isEqualTo(1);
        assertThat(publisher.published()).containsExactly(
                new ProviderCalendarChangedIE(PROVIDER, List.of(hours(9, 14)), Duration.ZERO, NOW));
    }

    @Test
    void syncedEventsBlockOpenTimeAndAreAnnounced() {
        availability.set(new SetAvailabilityRequest(PROVIDER, List.of(hours(9, 14))));

        List<TimeRange> open = sync.sync(new SyncCalendarEventsRequest(PROVIDER, List.of(hours(10, 11)))).openTime();

        assertThat(open).containsExactly(hours(9, 10), hours(11, 14));
        assertThat(calendars.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(publisher.published(ProviderCalendarChangedIE.class)).last().isEqualTo(
                new ProviderCalendarChangedIE(PROVIDER, List.of(hours(9, 10), hours(11, 14)), Duration.ZERO, NOW));
    }

    @Test
    void aChangedBufferIsSavedAndAnnounced() {
        availability.set(new SetAvailabilityRequest(PROVIDER, List.of(hours(9, 14))));

        Duration saved = buffer.set(new SetBufferRequest(PROVIDER, Duration.ofMinutes(45))).buffer();

        assertThat(saved).isEqualTo(Duration.ofMinutes(45));
        assertThat(calendars.timesSaved(PROVIDER)).isEqualTo(2);
        assertThat(publisher.published(ProviderCalendarChangedIE.class)).last().isEqualTo(
                new ProviderCalendarChangedIE(PROVIDER, List.of(hours(9, 14)), Duration.ofMinutes(45), NOW));
    }

    @Test
    void aNegativeBufferIsNeitherSavedNorAnnounced() {
        assertThatThrownBy(() -> buffer.set(new SetBufferRequest(PROVIDER, Duration.ofMinutes(-1))))
                .isInstanceOf(InvalidBufferException.class);
        assertThat(calendars.all()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    void aProviderSeesTheirCalendar() {
        availability.set(new SetAvailabilityRequest(PROVIDER, List.of(hours(9, 14))));
        sync.sync(new SyncCalendarEventsRequest(PROVIDER, List.of(hours(10, 11))));
        buffer.set(new SetBufferRequest(PROVIDER, Duration.ofMinutes(30)));

        ViewCalendarResponse view = new ProviderViewCalendar(calendars).view(new ViewCalendarRequest(PROVIDER));

        assertThat(view).isEqualTo(new ViewCalendarResponse(List.of(hours(9, 14)), List.of(hours(10, 11)),
                Duration.ofMinutes(30), List.of(hours(9, 10), hours(11, 14))));
    }

    @Test
    void aProviderWhoHasSetNothingSeesAnEmptyCalendar() {
        ViewCalendarResponse view = new ProviderViewCalendar(calendars).view(new ViewCalendarRequest(PROVIDER));

        assertThat(view).isEqualTo(new ViewCalendarResponse(List.of(), List.of(), Duration.ZERO, List.of()));
        assertThat(calendars.all()).isEmpty();
    }
}
