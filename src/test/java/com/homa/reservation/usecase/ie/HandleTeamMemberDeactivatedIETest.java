package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.NURUL_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.RINA_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.RosterNeedsReplacementIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.usecase.ReservationTestBed;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleTeamMemberDeactivatedIETest {

    private static final Instant WEDNESDAY_10_00 = HomaFixtures.at(2026, 12, 2, 10, 0);

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    /** Saturday's reservation, accepted, with Siti and Nurul rostered on Wednesday. */
    private Reservation rosteredSaturday() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(HomaFixtures.at(2026, 12, 2, 9, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        bed.publisher.clear();
        return reserved;
    }

    /** Mei's Wednesday 10:00 job, 2 helpers for 4 hours, accepted, with Siti and Nurul rostered too. */
    private Reservation rosteredWednesdayOfMei() {
        bed.time.set(monday(20, 29));
        String checkoutId = bed.startCheckout.start(new StartReservationCheckoutRequest(MEI_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING), WEDNESDAY_10_00, 240, 2)).checkoutId();
        bed.time.set(monday(20, 30));
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, monday(20, 30)));
        Reservation requested = bed.reservations.findByParty(MEI_CUSTOMER_ID).get(0);
        bed.time.set(monday(22, 0));
        bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), AZMAN_PERSON_ID));
        bed.assignRoster.assign(new AssignRosterRequest(requested.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        return bed.reload(requested);
    }

    private TeamMemberDeactivatedIE deactivated(String eventId, String memberId, String personId, String reason,
            Instant at) {
        return new TeamMemberDeactivatedIE(eventId, memberId, personId, KEMAS_HOME_PROVIDER_ID, reason, at);
    }

    @Test
    @Tag("HLP-6")
    @Tag("ROS-5")
    @DisplayName("Nurul, deactivated on Thursday, has to be replaced on Saturday's roster before 9:45.")
    void aDeactivatedMemberCannotBeRosteredAndTheProviderIsToldToReplaceThem() {
        Reservation reserved = rosteredSaturday();
        bed.time.set(HomaFixtures.at(2026, 12, 3, 10, 0));

        bed.teamMemberDeactivated.handle(
                deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", bed.time.now()));

        assertThat(bed.members.findById(NURUL_MEMBER_ID)).contains(new RosterableMember(NURUL_MEMBER_ID,
                NURUL_PERSON_ID, KEMAS_HOME_PROVIDER_ID, MemberRole.HELPER, "Nurul", true, Optional.of("photo-nurul"),
                false, Optional.empty()));
        assertThat(bed.members.findById(SITI_MEMBER_ID).orElseThrow().active()).isTrue();
        Reservation saved = bed.reload(reserved);
        assertThat(saved.roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(saved.roster().orElseThrow().isComplete()).isFalse();
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.publisher.single(RosterNeedsReplacementIE.class)).isEqualTo(new RosterNeedsReplacementIE(
                "reservation-event-5", reserved.id().value(), KEMAS_HOME_PROVIDER_ID, NURUL_MEMBER_ID,
                saturday(9, 45), HomaFixtures.at(2026, 12, 3, 10, 0)));
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-5")
    @DisplayName("Had she resigned on Wednesday, she could be removed then and replaced (ROS-5).")
    void aRemovedMemberIsReplacedLikeADeactivatedOne() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(HomaFixtures.at(2026, 12, 1, 22, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, RINA_MEMBER_ID)));
        bed.publisher.clear();
        bed.time.set(WEDNESDAY_10_00);

        bed.teamMemberDeactivated
                .handle(deactivated("removed-1", RINA_MEMBER_ID, RINA_PERSON_ID, "REMOVED", WEDNESDAY_10_00));

        assertThat(bed.members.findById(RINA_MEMBER_ID).orElseThrow().active()).isFalse();
        assertThat(bed.reload(reserved).roster().orElseThrow().membersToReplace()).containsExactly(RINA_MEMBER_ID);
        assertThat(bed.publisher.single(RosterNeedsReplacementIE.class).memberId()).isEqualTo(RINA_MEMBER_ID);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Nurul is on two upcoming rosters, Saturday's and Mei's Wednesday: both are flagged and the provider is told of each.")
    void everyUpcomingRosterWithTheMemberIsFlagged() {
        Reservation saturdays = rosteredSaturday();
        Reservation meis = rosteredWednesdayOfMei();
        bed.publisher.clear();
        bed.time.set(monday(23, 0));

        bed.teamMemberDeactivated
                .handle(deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", monday(23, 0)));

        assertThat(bed.reload(saturdays).roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(bed.reload(meis).roster().orElseThrow().membersToReplace()).containsExactly(NURUL_MEMBER_ID);
        assertThat(bed.publisher.published()).hasSize(2).allSatisfy(event -> {
            RosterNeedsReplacementIE needsReplacing = (RosterNeedsReplacementIE) event;
            assertThat(needsReplacing.memberId()).isEqualTo(NURUL_MEMBER_ID);
            assertThat(needsReplacing.providerId()).isEqualTo(KEMAS_HOME_PROVIDER_ID);
        });
        assertThat(bed.publisher.published()).extracting(event -> ((RosterNeedsReplacementIE) event).reservationId())
                .containsExactlyInAnyOrder(saturdays.id().value(), meis.id().value());
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("A roster without Nurul isn't flagged when she is deactivated.")
    void aRosterWithoutTheMemberIsLeftAlone() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(HomaFixtures.at(2026, 12, 2, 9, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, RINA_MEMBER_ID)));
        bed.publisher.clear();

        bed.teamMemberDeactivated.handle(deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED",
                bed.time.now()));

        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Nurul is deactivated at 9:45 on Saturday, when the roster has locked: she can't be rostered again, and nothing is flagged.")
    void aRosterInItsCheckInPeriodIsLockedAndNotFlagged() {
        Reservation reserved = rosteredSaturday();
        bed.time.set(saturday(9, 45));

        bed.teamMemberDeactivated.handle(
                deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", saturday(9, 45)));

        assertThat(bed.members.findById(NURUL_MEMBER_ID).orElseThrow().active()).isFalse();
        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("A reservation Aisyah cancelled before Nurul was deactivated has nothing to replace.")
    void aCancelledReservationIsNotFlagged() {
        Reservation reserved = rosteredSaturday();
        bed.time.set(friday(10, 0));
        bed.customerCancel.cancel(new CustomerCancelReservationRequest(reserved.id().value(),
                HomaFixtures.AISYAH_CUSTOMER_ID));
        bed.publisher.clear();

        bed.teamMemberDeactivated.handle(
                deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", friday(11, 0)));

        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("Reservation hasn't heard of the member: nothing is stored and nothing is flagged.")
    void aMemberReservationNeverHeardOfChangesNothing() {
        Reservation reserved = rosteredSaturday();
        int membersBefore = bed.members.count();

        bed.teamMemberDeactivated.handle(
                deactivated("deactivated-1", "member-unknown", "person-unknown", "DEACTIVATED", bed.time.now()));

        assertThat(bed.members.count()).isEqualTo(membersBefore);
        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("When Kemas Home swaps Nurul out while she is being flagged, the flag finds her gone and nothing is published.")
    void aSwapCommittingFirstIsSeenOnTheRetry() {
        Reservation reserved = rosteredSaturday();
        bed.reservations.beforeNextSave(() -> bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(),
                AZMAN_PERSON_ID, List.of(SITI_MEMBER_ID, RINA_MEMBER_ID))));
        bed.publisher.clear();

        bed.teamMemberDeactivated.handle(
                deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", bed.time.now()));

        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.reload(reserved).roster().orElseThrow().includes(NURUL_MEMBER_ID)).isFalse();
        assertThat(bed.publisher.published()).hasSize(1);
        assertThat(bed.publisher.published().get(0)).isNotInstanceOf(RosterNeedsReplacementIE.class);
    }

    @Test
    @Tag("ROS-5")
    @DisplayName("A redelivered deactivation changes nothing and publishes nothing: Nurul, back on Friday, stays active.")
    void redeliveredDeactivationIsIgnored() {
        Reservation reserved = rosteredSaturday();
        TeamMemberDeactivatedIE deactivation =
                deactivated("deactivated-1", NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED", bed.time.now());
        bed.teamMemberDeactivated.handle(deactivation);
        bed.teamMemberActivated.handle(new TeamMemberActivatedIE(bed.nextInboundId(), NURUL_MEMBER_ID,
                NURUL_PERSON_ID, KEMAS_HOME_PROVIDER_ID, "Nurul", "HELPER", "LOCAL", true, null, bed.time.now()));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        bed.publisher.clear();

        bed.teamMemberDeactivated.handle(deactivation);

        assertThat(bed.members.findById(NURUL_MEMBER_ID).orElseThrow().active()).isTrue();
        assertThat(bed.reload(reserved).roster().orElseThrow().isComplete()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
        assertThat(HandleTeamMemberDeactivatedIE.HANDLER).isEqualTo("reservation.TeamMemberDeactivated");
    }
}
