package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.thursday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.roster.RosterMember;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** ROS-5: a rostered member who stops being active before the Check-in Period is replaced by the provider. */
class ReservationRosterReplacementTest {

    private final Reservation reservation = Marketplace.withKemasHome().reserveStandard();

    private Reservation rostered() {
        reservation.assignRoster(List.of(siti(), nurul()), thursday(9, 0));
        return reservation;
    }

    private List<String> memberIds() {
        return reservation.roster().orElseThrow().members().stream().map(RosterMember::memberId).toList();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Nurul is suspended on Friday. Kemas Home rosters Rina before Saturday 9:45.")
    void aMemberWhoStopsBeingActiveIsFlaggedAndTheProviderReplacesThemBeforeCheckIn() {
        rostered();

        boolean flagged = reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0));

        assertThat(flagged).isTrue();
        assertThat(reservation.roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(reservation.roster().orElseThrow().isComplete()).isFalse();
        assertThat(memberIds()).as("she stays on the roster until replaced").containsExactly(SITI_MEMBER_ID,
                NURUL_MEMBER_ID);

        reservation.assignRoster(List.of(siti(), rina()), saturday(8, 0));

        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
        assertThat(reservation.roster().orElseThrow().membersToReplace()).isEmpty();
        assertThat(reservation.roster().orElseThrow().isComplete()).isTrue();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("At 9:44 Nurul stops being active and Kemas Home can still replace her; at 9:45 the roster is locked and nothing is flagged.")
    void aMemberLostInTheCheckInPeriodIsNotFlaggedBecauseTheRosterIsLocked() {
        rostered();

        assertThat(reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, saturday(9, 45))).isFalse();
        assertThat(reservation.roster().orElseThrow().membersToReplace()).isEmpty();

        assertThat(reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, saturday(9, 44))).isTrue();
        assertThat(reservation.roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Rina, not on Saturday's roster, stops being active: nothing to replace.")
    void aMemberNotOnTheRosterChangesNothing() {
        rostered();

        assertThat(reservation.rosterMemberStoppedBeingActive(RINA_MEMBER_ID, friday(10, 0))).isFalse();
        assertThat(reservation.roster().orElseThrow().isComplete()).isTrue();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Before any roster is set there is no one to replace.")
    void aReservationWithoutARosterHasNoOneToReplace() {
        assertThat(reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0))).isFalse();
        assertThat(reservation.roster()).isEmpty();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Nurul is flagged once: hearing again that she isn't active changes nothing.")
    void aMemberAlreadyFlaggedIsNotFlaggedAgain() {
        rostered();
        reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0));

        assertThat(reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(11, 0))).isFalse();
        assertThat(reservation.roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Both Siti and Nurul stop being active: both are flagged, and the roster is complete again only when both are replaced.")
    void severalMembersCanBeFlaggedAtOnce() {
        rostered();

        reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0));
        reservation.rosterMemberStoppedBeingActive(SITI_MEMBER_ID, friday(11, 0));

        assertThat(reservation.roster().orElseThrow().membersToReplace())
                .containsExactlyInAnyOrder(NURUL_MEMBER_ID, SITI_MEMBER_ID);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Kemas Home cancelled on Friday: a member who stops being active later is no one's business on that reservation.")
    void aReservationThatIsNoLongerReservedIsNotFlagged() {
        rostered();
        reservation.cancelByProvider(friday(8, 0));

        assertThat(reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0))).isFalse();
        assertThat(reservation.roster().orElseThrow().membersToReplace()).isEmpty();
    }

    @Test
    @Tag("HLP-6")
    @DisplayName("Nurul, deactivated on Thursday, can't be rostered, so Kemas Home must roster someone else: she can't be put back while deactivated.")
    void aDeactivatedMemberCannotBeRosteredAgainUntilActive() {
        rostered();
        reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0));

        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), deactivated(nurul())), friday(11, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
        assertThat(reservation.roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);

        reservation.assignRoster(List.of(siti(), nurul()), friday(12, 0));

        assertThat(reservation.roster().orElseThrow().isComplete()).as("she was reactivated").isTrue();
    }

    @Test
    @Tag("HLP-6")
    @DisplayName("A deactivated member is the same member, not active: nothing else about Nurul changes, and she can't work Saturday.")
    void deactivatingAMemberChangesOnlyWhetherTheyAreActive() {
        assertThat(nurul().deactivated()).isEqualTo(deactivated(nurul()));
        assertThat(nurul().canWorkOn(saturday(14, 0))).isTrue();
        assertThat(nurul().deactivated().canWorkOn(saturday(14, 0))).isFalse();
    }
}
