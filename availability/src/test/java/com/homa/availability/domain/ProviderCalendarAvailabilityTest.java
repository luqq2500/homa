package com.homa.availability.domain;

import static com.homa.availability.domain.AvailabilityBuilders.aCalendarAvailable;
import static com.homa.availability.domain.AvailabilityBuilders.anOpenSaturday;
import static com.homa.availability.domain.AvailabilityBuilders.busy;
import static com.homa.availability.domain.AvailabilityBuilders.slot;
import static com.homa.availability.domain.AvailabilityBuilders.window;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.availability.domain.exception.AvailabilityDetailRequiredException;
import com.homa.availability.domain.exception.NoHoldForReservationException;
import com.homa.availability.domain.exception.ReservationAlreadyHeldException;
import com.homa.availability.domain.exception.SlotNotAvailableException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Provider availability. */
class ProviderCalendarAvailabilityTest {

    @Nested
    class Windows {

        @Test
        void aSlotInsideAWindowWithNothingElseIsAvailable() {
            assertThat(anOpenSaturday().isAvailable(slot("10:00", "13:00"))).isTrue();
        }

        @Test
        void aSlotPartlyOutsideTheWindowIsNotAvailable() {
            assertThat(aCalendarAvailable("08:00", "12:00").isAvailable(slot("10:00", "13:00"))).isFalse();
            assertThat(aCalendarAvailable("11:00", "18:00").isAvailable(slot("10:00", "13:00"))).isFalse();
        }

        @Test
        void aSlotFillingTheWholeWindowIsAvailable() {
            assertThat(aCalendarAvailable("10:00", "13:00").isAvailable(slot("10:00", "13:00"))).isTrue();
        }

        @Test
        void aProviderWithNoWindowsIsNeverAvailable() {
            assertThat(ProviderCalendar.of("provider-1").isAvailable(slot("10:00", "13:00"))).isFalse();
        }

        @Test
        void windowsThatTouchCountAsOneStretch() {
            ProviderCalendar calendar = ProviderCalendar.of("provider-1");
            calendar.setWindows(List.of(window("12:00", "18:00"), window("08:00", "12:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isTrue();
            assertThat(calendar.windows()).containsExactly(window("08:00", "12:00"), window("12:00", "18:00"));
        }

        @Test
        void aWindowInsideAnotherDoesNotCutItShort() {
            ProviderCalendar calendar = ProviderCalendar.of("provider-1");
            calendar.setWindows(List.of(window("08:00", "18:00"), window("09:00", "10:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isTrue();
        }

        @Test
        void aGapBetweenWindowsMakesTheSlotUnavailable() {
            ProviderCalendar calendar = ProviderCalendar.of("provider-1");
            calendar.setWindows(List.of(window("08:00", "11:00"), window("11:15", "18:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isFalse();
        }

        @Test
        void newWindowsReplaceTheOldOnes() {
            ProviderCalendar calendar = anOpenSaturday();

            calendar.setWindows(List.of(window("14:00", "18:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isFalse();
            assertThat(calendar.isAvailable(slot("14:00", "17:00"))).isTrue();
        }
    }

    @Nested
    class BusyBlocks {

        @Test
        void aSyncedBusyBlockMakesTheSlotUnavailable() {
            ProviderCalendar calendar = anOpenSaturday();

            calendar.replaceBusyBlocks(List.of(busy("12:00", "13:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isFalse();
        }

        @Test
        void aBusyBlockThatOnlyTouchesTheSlotLeavesItAvailable() {
            ProviderCalendar calendar = anOpenSaturday();

            calendar.replaceBusyBlocks(List.of(busy("09:00", "10:00"), busy("13:00", "14:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isTrue();
        }

        @Test
        void eachSyncReplacesTheLast() {
            ProviderCalendar calendar = anOpenSaturday();
            calendar.replaceBusyBlocks(List.of(busy("12:00", "13:00")));

            calendar.replaceBusyBlocks(List.of(busy("15:00", "16:00")));

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isTrue();
            assertThat(calendar.busyBlocks()).containsExactly(busy("15:00", "16:00"));
        }
    }

    @Nested
    class ReservationHolds {

        @Test
        void anAcceptedReservationHoldsTheSlot() {
            ProviderCalendar calendar = anOpenSaturday();

            calendar.hold("R1", slot("10:00", "13:00"));

            assertThat(calendar.isAvailable(slot("11:00", "12:00"))).isFalse();
            assertThat(calendar.isAvailable(slot("13:00", "14:00"))).isTrue();
            assertThat(calendar.holds()).containsExactly(new ReservationHold("R1", slot("10:00", "13:00")));
        }

        @Test
        void releasingACancelledReservationFreesTheSlot() {
            ProviderCalendar calendar = anOpenSaturday();
            calendar.hold("R1", slot("10:00", "13:00"));

            calendar.release("R1");

            assertThat(calendar.isAvailable(slot("10:00", "13:00"))).isTrue();
            assertThat(calendar.holds()).isEmpty();
        }

        @Test
        void holdingAnOverlappingSlotIsRejected() {
            ProviderCalendar calendar = anOpenSaturday();
            calendar.hold("R1", slot("10:00", "13:00"));

            assertThatThrownBy(() -> calendar.hold("R2", slot("12:00", "14:00")))
                    .isInstanceOf(SlotNotAvailableException.class);
            assertThat(calendar.holds()).hasSize(1);
        }

        @Test
        void holdingASlotOutsideTheWindowsIsRejected() {
            assertThatThrownBy(() -> aCalendarAvailable("08:00", "12:00").hold("R1", slot("10:00", "13:00")))
                    .isInstanceOf(SlotNotAvailableException.class);
        }

        @Test
        void aReservationHoldsOnlyOneSlot() {
            ProviderCalendar calendar = anOpenSaturday();
            calendar.hold("R1", slot("10:00", "13:00"));

            assertThatThrownBy(() -> calendar.hold("R1", slot("14:00", "16:00")))
                    .isInstanceOf(ReservationAlreadyHeldException.class);
        }

        @Test
        void onlyAHeldReservationCanBeReleased() {
            assertThatThrownBy(() -> anOpenSaturday().release("R1"))
                    .isInstanceOf(NoHoldForReservationException.class)
                    .hasMessageContaining("R1");
        }

        @Test
        void aHoldNeedsAReservationAndASlot() {
            assertThatThrownBy(() -> new ReservationHold(null, slot("10:00", "13:00"))).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ReservationHold("R1", null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void aCalendarBelongsToAProvider() {
        ProviderCalendar calendar = ProviderCalendar.of("provider-1");

        assertThat(calendar.providerId()).isEqualTo("provider-1");
        assertThat(calendar.id()).isNotBlank();
        assertThatThrownBy(() -> ProviderCalendar.of(" ")).isInstanceOf(AvailabilityDetailRequiredException.class);
        assertThatThrownBy(() -> ProviderCalendar.of(null)).isInstanceOf(AvailabilityDetailRequiredException.class);
    }
}
