package com.homa.provider.usecase.ie;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_CHECK_IN_START;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_JOB_END;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_RESERVATION;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.completed;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rosterChanged;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.saturdayReserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.TimeInterval;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.RosteredReservation;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.ProviderWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleRosterChangedIETest {

    private static final ReservationId SATURDAY = new ReservationId(SATURDAY_RESERVATION);
    private static final TeamMemberId SITI = new TeamMemberId("member-siti");
    private static final TeamMemberId NURUL = new TeamMemberId("member-nurul");
    private static final TeamMemberId RINA = new TeamMemberId("member-rina");
    private static final TimeInterval WINDOW = new TimeInterval(SATURDAY_CHECK_IN_START, SATURDAY_JOB_END);

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void saturdayIsReserved() {
        world.reservationReserved.handle(saturdayReserved("reserved-1"));
    }

    private RosteredReservation saturday() {
        return world.rosteredWindows.findByReservationId(SATURDAY).orElseThrow();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-1")
    @DisplayName("Kemas Home rosters Siti and Nurul on Friday: each has Saturday's window, from 9:45 to 14:00.")
    void rosteredMembersHaveTheReservationsWindow() {
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti", "member-nurul"));

        assertThat(saturday().roster()).containsExactlyInAnyOrder(SITI, NURUL);
        assertThat(saturday().window()).isEqualTo(WINDOW);
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).containsExactly(WINDOW);
        assertThat(world.rosteredWindows.windowsOf(NURUL).windows()).containsExactly(WINDOW);
        assertThat(world.rosteredWindows.windowsOf(RINA).windows()).isEmpty();
        assertThat(world.processedMessages.hasHandled("roster-1", "provider.RosterChanged")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-1")
    @DisplayName("Kemas Home swaps Nurul for Rina at 8:00 on Saturday: Nurul's window goes and Rina's comes.")
    void aRosterChangeReplacesTheRoster() {
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti", "member-nurul"));

        world.rosterChanged.handle(rosterChanged("roster-2", SATURDAY_RESERVATION, "member-siti", "member-rina"));

        assertThat(saturday().roster()).containsExactlyInAnyOrder(SITI, RINA);
        assertThat(world.rosteredWindows.windowsOf(NURUL).windows()).isEmpty();
        assertThat(world.rosteredWindows.windowsOf(RINA).windows()).containsExactly(WINDOW);
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A roster delivered again changes nothing: the later roster Kemas Home set stays.")
    void redeliveredEventChangesNothing() {
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti", "member-nurul"));
        world.rosterChanged.handle(rosterChanged("roster-2", SATURDAY_RESERVATION, "member-siti", "member-rina"));

        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti", "member-nurul"));

        assertThat(saturday().roster()).containsExactlyInAnyOrder(SITI, RINA);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A roster for a reservation Provider holds no window for has nothing to attach to: it is recorded as handled and dropped.")
    void rosterForAnUnknownReservationIsDropped() {
        world.rosterChanged.handle(rosterChanged("roster-x", "reservation-unknown", "member-siti"));

        assertThat(world.rosteredWindows.findByReservationId(new ReservationId("reservation-unknown"))).isEmpty();
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).isEmpty();
        assertThat(world.rosteredWindows.count()).isEqualTo(1);
        assertThat(world.processedMessages.hasHandled("roster-x", "provider.RosterChanged")).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("A roster that arrives after the job was Completed doesn't bring the window back.")
    void rosterAfterTheReservationClosedIsDropped() {
        world.reservationCompleted.handle(completed("completed-1", SATURDAY_RESERVATION));

        world.rosterChanged.handle(rosterChanged("roster-late", SATURDAY_RESERVATION, "member-siti"));

        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).isEmpty();
        assertThat(world.rosteredWindows.count()).isZero();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("An empty roster leaves nobody holding the window.")
    void emptyRosterHoldsNobody() {
        world.rosterChanged.handle(rosterChanged("roster-1", SATURDAY_RESERVATION, "member-siti"));

        world.rosterChanged.handle(rosterChanged("roster-2", SATURDAY_RESERVATION));

        assertThat(saturday().roster()).isEmpty();
        assertThat(world.rosteredWindows.windowsOf(SITI).windows()).isEmpty();
    }
}
