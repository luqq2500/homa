package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;

import com.homa.ie.events.RosterChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.exception.IncompleteRosterException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.AssignRosterResponse;
import com.homa.reservation.usecase.dto.AssignRosterResponse.RosteredMember;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.TeamMemberNotFoundException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderAssignRosterTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private AssignRosterResponse roster(String actorId, String... memberIds) {
        return bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), actorId, List.of(memberIds)));
    }

    private void assertNothingChanges(ThrowingCallable call, Class<? extends Throwable> refusal) {
        bed.publisher.clear();
        assertThatThrownBy(call).isInstanceOf(refusal);
        assertThat(bed.reload(reserved).roster()).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Kemas Home rosters Siti and Nurul on Friday, and Aisyah is told of the roster.")
    void providerSetsTheRosterAndTheCustomerIsTold() {
        bed.publisher.clear();
        bed.time.set(friday(18, 0));

        AssignRosterResponse rostered = roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, NURUL_MEMBER_ID, FARID_MEMBER_ID);

        String id = reserved.id().value();
        assertThat(rostered).isEqualTo(new AssignRosterResponse(id, List.of(
                new RosteredMember(SITI_MEMBER_ID, "HELPER", "Siti", "photo-siti"),
                new RosteredMember(NURUL_MEMBER_ID, "HELPER", "Nurul", "photo-nurul"),
                new RosteredMember(FARID_MEMBER_ID, "SUPERVISOR", "Farid", "photo-farid"))));
        assertThat(bed.reload(reserved).roster().orElseThrow().members()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID, FARID_MEMBER_ID);
        assertThat(bed.publisher.single(RosterChangedIE.class)).isEqualTo(new RosterChangedIE(
                "reservation-event-4", id, List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID, FARID_MEMBER_ID),
                friday(18, 0)));
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Kemas Home swaps Nurul for Rina at 8:00 on Saturday, and the roster locks at 9:45.")
    void rosterCanChangeUntilTheCheckInPeriodAndThenLocks() {
        bed.time.set(friday(18, 0));
        roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, NURUL_MEMBER_ID);
        bed.publisher.clear();
        bed.time.set(saturday(8, 0));

        roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, RINA_MEMBER_ID);

        assertThat(bed.publisher.single(RosterChangedIE.class).memberIds())
                .containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));
        assertThatThrownBy(() -> roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, NURUL_MEMBER_ID))
                .isInstanceOf(RosterLockedException.class);
        assertThat(bed.reload(reserved).roster().orElseThrow().members()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-2")
    @DisplayName("Aisyah booked 2 helpers. Siti and Farid isn't a complete roster.")
    void rosterNeedsTheBookedNumberOfHelpers() {
        bed.time.set(friday(18, 0));

        assertNothingChanges(() -> roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, FARID_MEMBER_ID),
                IncompleteRosterException.class);
    }

    @Test
    @Tag("ROS-3")
    @DisplayName("Only members Reservation knows as activated can be rostered.")
    void unknownMemberCannotBeRostered() {
        bed.time.set(friday(18, 0));

        assertNothingChanges(() -> roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, "member-unknown"),
                TeamMemberNotFoundException.class);
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("A new helper whose photo isn't verified yet can't be rostered, because Aisyah must see each member's photo.")
    void memberWithoutAVerifiedPhotoCannotBeRostered() {
        bed.teamMemberActivated.handle(new TeamMemberActivatedIE(bed.nextInboundId(), "member-aina", "person-aina",
                KEMAS_HOME_PROVIDER_ID, "Aina", "HELPER", "LOCAL", false, null, TAPPED_PAY_AT));
        bed.time.set(friday(18, 0));

        assertNothingChanges(() -> roster(AZMAN_PERSON_ID, SITI_MEMBER_ID, "member-aina"),
                MemberNotRosterableException.class);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Only Kemas Home rosters its reservations; Aisyah can't.")
    void onlyTheReservationsProviderRosters() {
        bed.time.set(friday(18, 0));

        assertNothingChanges(() -> roster(AISYAH_CUSTOMER_ID, SITI_MEMBER_ID, NURUL_MEMBER_ID),
                NotReservationPartyException.class);
    }
}
