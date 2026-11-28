package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.CHECK_IN_PERIOD;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.domain.ProviderFixtures.NURUL;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RosteredWindowsTest {

    private static final ReservationId SATURDAY_JOB = new ReservationId("reservation-saturday");
    private static final Instant CHECK_IN_START = CHECK_IN_PERIOD.start();

    @Test
    @Tag("HLP-9")
    @DisplayName("A window runs from the start of the Check-in Period at 9:45 up to, not including, the job end at 14:00.")
    void windowRunsFromCheckInStartUpToTheJobEnd() {
        RosteredWindows saturday = RosteredWindows.of(List.of(new TimeInterval(CHECK_IN_START, JOB_END)));

        assertThat(saturday.isInsideOneAt(CHECK_IN_START.minusSeconds(1))).isFalse();
        assertThat(saturday.isInsideOneAt(CHECK_IN_START)).isTrue();
        assertThat(saturday.isInsideOneAt(at(2026, 12, 5, 10, 0))).isTrue();
        assertThat(saturday.isInsideOneAt(JOB_END.minusSeconds(1))).isTrue();
        assertThat(saturday.isInsideOneAt(JOB_END)).isFalse();
        assertThat(saturday.isInsideOneAt(at(2026, 12, 5, 14, 1))).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A member rostered on nothing is never inside a window.")
    void memberRosteredOnNothingIsNeverInsideAWindow() {
        assertThat(RosteredWindows.none().windows()).isEmpty();
        assertThat(RosteredWindows.none().isInsideOneAt(at(2026, 12, 5, 11, 0))).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Being inside any one of a member's windows counts.")
    void anyOneWindowCounts() {
        RosteredWindows two = RosteredWindows.of(List.of(
                new TimeInterval(at(2026, 12, 5, 7, 45), at(2026, 12, 5, 9, 0)),
                new TimeInterval(CHECK_IN_START, JOB_END)));

        assertThat(two.isInsideOneAt(at(2026, 12, 5, 8, 0))).isTrue();
        assertThat(two.isInsideOneAt(at(2026, 12, 5, 9, 15))).isFalse();
        assertThat(two.isInsideOneAt(at(2026, 12, 5, 13, 0))).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("The windows are fixed when they are read; later changes to the list they were read from don't change them.")
    void windowsAreFixedWhenRead() {
        List<TimeInterval> source = new ArrayList<>(List.of(new TimeInterval(CHECK_IN_START, JOB_END)));
        RosteredWindows windows = RosteredWindows.of(source);

        source.clear();

        assertThat(windows.windows()).hasSize(1);
        assertThat(new RosteredWindows(source).windows()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A reservation just Reserved has its window from the Check-in Period to the job end, and nobody rostered.")
    void reservedReservationHasAWindowAndNoRoster() {
        RosteredReservation saturday = RosteredReservation.reserved(SATURDAY_JOB, CHECK_IN_START, JOB_END);

        assertThat(saturday.reservationId()).isEqualTo(SATURDAY_JOB);
        assertThat(saturday.window()).isEqualTo(new TimeInterval(CHECK_IN_START, JOB_END));
        assertThat(saturday.roster()).isEmpty();
        assertThat(saturday.windowOf(SITI)).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-1")
    @DisplayName("Only a member listed on the roster has a window: Siti and Nurul do, Rina doesn't.")
    void onlyARosteredMemberHasAWindow() {
        RosteredReservation saturday = RosteredReservation.reserved(SATURDAY_JOB, CHECK_IN_START, JOB_END)
                .rosteredOn(List.of(SITI, NURUL));

        assertThat(saturday.roster()).containsExactlyInAnyOrder(SITI, NURUL);
        assertThat(saturday.windowOf(SITI)).contains(new TimeInterval(CHECK_IN_START, JOB_END));
        assertThat(saturday.windowOf(NURUL)).contains(new TimeInterval(CHECK_IN_START, JOB_END));
        assertThat(saturday.windowOf(RINA)).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-1")
    @DisplayName("Kemas Home swaps Nurul for Rina: the new roster replaces the old, so Nurul's window goes and Rina's comes.")
    void aRosterChangeReplacesTheRoster() {
        RosteredReservation saturday = RosteredReservation.reserved(SATURDAY_JOB, CHECK_IN_START, JOB_END)
                .rosteredOn(List.of(SITI, NURUL))
                .rosteredOn(List.of(SITI, RINA));

        assertThat(saturday.roster()).containsExactlyInAnyOrder(SITI, RINA);
        assertThat(saturday.windowOf(NURUL)).isEmpty();
        assertThat(saturday.windowOf(RINA)).isPresent();
        assertThat(saturday.reservationId()).isEqualTo(SATURDAY_JOB);
        assertThat(saturday.window()).isEqualTo(new TimeInterval(CHECK_IN_START, JOB_END));
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A reservation needs its id and window, and the roster is fixed when it's given.")
    void reservationNeedsItsIdAndWindowAndKeepsItsRoster() {
        TimeInterval window = new TimeInterval(CHECK_IN_START, JOB_END);
        Set<TeamMemberId> roster = Set.of(SITI);

        assertThatThrownBy(() -> new RosteredReservation(null, window, roster))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RosteredReservation(SATURDAY_JOB, null, roster))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RosteredReservation.reserved(SATURDAY_JOB, JOB_END, CHECK_IN_START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReservationId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReservationId(null)).isInstanceOf(NullPointerException.class);
    }
}
