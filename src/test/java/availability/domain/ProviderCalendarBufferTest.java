package availability.domain;

import static availability.domain.AvailabilityBuilders.anOpenSaturday;
import static availability.domain.AvailabilityBuilders.busy;
import static availability.domain.AvailabilityBuilders.saturday;
import static availability.domain.AvailabilityBuilders.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import availability.domain.exception.NegativeBufferBetweenJobsException;
import availability.domain.exception.SlotNotAvailableException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Feature: Buffer between jobs. */
class ProviderCalendarBufferTest {

    private static final Duration HALF_AN_HOUR = Duration.ofMinutes(30);

    private ProviderCalendar aCalendarWithR1From10To13AndABufferOf(Duration buffer) {
        ProviderCalendar calendar = anOpenSaturday();
        calendar.hold("R1", slot("10:00", "13:00"));
        calendar.setBufferBetweenJobs(buffer);
        return calendar;
    }

    @Test
    void aProviderHasNoBufferUntilTheySetOne() {
        ProviderCalendar calendar = anOpenSaturday();
        calendar.hold("R1", slot("10:00", "13:00"));

        assertThat(calendar.bufferBetweenJobs()).isEqualTo(Duration.ZERO);
        assertThat(calendar.isAvailable(slot("13:00", "14:00"))).isTrue();
        assertThat(calendar.isAvailable(slot("09:00", "10:00"))).isTrue();
    }

    @Test
    void aJobCannotStartWithinTheBufferAfterAnotherJob() {
        ProviderCalendar calendar = aCalendarWithR1From10To13AndABufferOf(HALF_AN_HOUR);

        assertThat(calendar.isAvailable(slot("13:00", "14:00"))).isFalse();
        assertThat(calendar.isAvailable(slot("13:15", "14:15"))).isFalse();
        assertThat(calendar.isAvailable(slot("13:30", "14:30"))).isTrue();
    }

    @Test
    void aJobCannotEndWithinTheBufferBeforeAnotherJob() {
        ProviderCalendar calendar = aCalendarWithR1From10To13AndABufferOf(HALF_AN_HOUR);

        assertThat(calendar.isAvailable(slot("08:30", "09:45"))).isFalse();
        assertThat(calendar.isAvailable(slot("08:30", "09:30"))).isTrue();
    }

    @Test
    void holdingASlotInsideTheBufferIsRejected() {
        ProviderCalendar calendar = aCalendarWithR1From10To13AndABufferOf(HALF_AN_HOUR);

        assertThatThrownBy(() -> calendar.hold("R2", slot("13:15", "15:15"))).isInstanceOf(SlotNotAvailableException.class);
        calendar.hold("R2", slot("13:30", "15:30"));
        assertThat(calendar.holds()).hasSize(2);
    }

    @Test
    void theBufferIsKeptAroundJobsNotAroundSyncedBusyBlocks() {
        ProviderCalendar calendar = aCalendarWithR1From10To13AndABufferOf(HALF_AN_HOUR);
        calendar.replaceBusyBlocks(List.of(busy("14:00", "15:00")));

        assertThat(calendar.isAvailable(slot("15:00", "16:00"))).isTrue();
        assertThat(calendar.isAvailable(slot("13:30", "14:00"))).isTrue();
    }

    @Test
    void theNearestStartRespectsTheBuffer() {
        ProviderCalendar calendar = aCalendarWithR1From10To13AndABufferOf(HALF_AN_HOUR);

        assertThat(calendar.nearestAvailableStart(slot("13:00", "14:00"), Duration.ofHours(2))).contains(saturday("13:30"));
    }

    @Test
    void theBufferCannotBeNegative() {
        ProviderCalendar calendar = anOpenSaturday();

        assertThatThrownBy(() -> calendar.setBufferBetweenJobs(Duration.ofMinutes(-15)))
                .isInstanceOf(NegativeBufferBetweenJobsException.class);
        calendar.setBufferBetweenJobs(HALF_AN_HOUR);
        assertThat(calendar.bufferBetweenJobs()).isEqualTo(HALF_AN_HOUR);
    }
}
