package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.withoutVerifiedPhoto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.IncompleteRosterException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.roster.RosterMember;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationRosterTest {

    private final Reservation reservation = Marketplace.withKemasHome().reserveStandard();

    @Test
    @Tag("ROS-1")
    @DisplayName("Kemas Home rosters Siti and Nurul on Friday, swaps Nurul for Rina at 8:00 on Saturday, and the roster locks at 9:45.")
    void providerSetsAndChangesTheRosterUntilTheCheckInPeriodStarts() {
        reservation.assignRoster(List.of(siti(), nurul()), friday(19, 0));
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);

        reservation.assignRoster(List.of(siti(), rina()), saturday(8, 0));
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);

        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), nurul()), saturday(9, 45)))
                .isInstanceOf(RosterLockedException.class);
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("At 9:44 the roster can still change.")
    void rosterCanChangeUntilTheMinuteBeforeCheckIn() {
        reservation.assignRoster(List.of(siti(), nurul()), saturday(9, 44));

        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A request is rostered once it is Reserved.")
    void requestedReservationCannotBeRosteredYet() {
        Reservation requested = Marketplace.withKemasHome().requestStandard();

        assertThatThrownBy(() -> requested.assignRoster(List.of(siti(), nurul()), friday(19, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("ROS-2")
    @DisplayName("Aisyah booked 2 helpers. Siti, Nurul and Farid is complete; Siti and Farid isn't.")
    void rosterIncludesTheBookedNumberOfHelpersAndSupervisorsAreExtra() {
        reservation.assignRoster(List.of(siti(), nurul(), farid()), friday(19, 0));

        Roster roster = reservation.roster().orElseThrow();
        assertThat(roster.helpers()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), farid()), friday(20, 0)))
                .isInstanceOf(IncompleteRosterException.class);
    }

    @Test
    @Tag("ROS-2")
    @DisplayName("Three helpers for a 2-helper booking, or Siti twice, is not the booked roster.")
    void rosterWithTooManyHelpersOrARepeatedMemberIsRefused() {
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), nurul(), rina()), friday(19, 0)))
                .isInstanceOf(IncompleteRosterException.class);
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), siti()), friday(19, 0)))
                .isInstanceOf(IncompleteRosterException.class);
        assertThat(reservation.roster()).isEmpty();
    }

    @Test
    @Tag("ROS-3")
    @DisplayName("Nurul is suspended, so Kemas Home rosters Siti and Rina.")
    void onlyActiveMembersCanBeRostered() {
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), deactivated(nurul())), friday(19, 0)))
                .isInstanceOf(MemberNotRosterableException.class);

        reservation.assignRoster(List.of(siti(), rina()), friday(19, 0));
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
    }

    @Test
    @Tag("ROS-3")
    @DisplayName("Only Kemas Home's own team can be rostered on its reservation.")
    void anotherProvidersMemberCannotBeRostered() {
        RosterableMember stranger = RosterableMember.local("member-stranger", "person-stranger", "provider-another",
                MemberRole.HELPER, "Stranger", "photo-stranger", true);

        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), stranger), friday(19, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("On Friday Aisyah sees Siti, Nurul and Farid.")
    void rosterIsShownWithEachMembersNameAndPhoto() {
        reservation.assignRoster(List.of(siti(), nurul(), farid()), friday(19, 0));

        assertThat(reservation.roster().orElseThrow().members()).containsExactly(
                new RosterMember(SITI_MEMBER_ID, "person-siti", MemberRole.HELPER, "Siti", "photo-siti"),
                new RosterMember(NURUL_MEMBER_ID, "person-nurul", MemberRole.HELPER, "Nurul", "photo-nurul"),
                new RosterMember(FARID_MEMBER_ID, "person-farid", MemberRole.SUPERVISOR, "Farid", "photo-farid"));
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Nurul's photo isn't verified yet, so she can't be rostered until it is: Aisyah must see each rostered member's photo.")
    void memberWithoutAVerifiedPhotoCannotBeRostered() {
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), withoutVerifiedPhoto(nurul())), friday(19, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
        assertThat(reservation.roster()).isEmpty();

        reservation.assignRoster(List.of(siti(), withoutVerifiedPhoto(nurul()).withVerifiedPhoto("photo-nurul")),
                friday(19, 0));
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);
    }

    private List<String> memberIds() {
        return reservation.roster().orElseThrow().members().stream().map(RosterMember::memberId).toList();
    }
}
