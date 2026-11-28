package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.thursday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.IncompleteRosterException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.NoSmallerRosterProposedException;
import com.homa.reservation.domain.exception.NotASmallerRosterException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.exception.SmallerRosterAlreadyProposedException;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.RosterAdjustment;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.domain.roster.RosterProposal;
import com.homa.reservation.domain.settlement.Settlement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** ROS-8: a provider short of helpers offers a smaller roster before the Check-in Period, and the customer answers. */
class ReservationSmallerRosterTest {

    private static final Settlement SITI_ALONE_ON_THE_STANDARD_RESERVATION =
            new Settlement(Money.myr(110), Money.myr(90), Money.myr(0), Money.myr(0), Money.myr(110));

    private final Reservation reservation = Marketplace.withKemasHome().reserveStandard();

    /** Aisyah books 3 helpers for the same job under Kemas Home's 10% discount: 25 × 4 × 3 × 0.9 + 20 = RM290. */
    private Reservation threeHelpers() {
        Reservation booked = Marketplace.withKemasHome()
                .book(aisyahBooking(JOB_START, STANDARD_HOURS, 3), TAPPED_PAY_AT, REQUESTED_AT);
        booked.accept(ACCEPTED_AT);
        return booked;
    }

    private List<String> memberIds() {
        return reservation.roster().orElseThrow().members().stream().map(RosterMember::memberId).toList();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti alone. Aisyah accepts: 25 × 4 × 1 × 0.9 + 20 = RM110, so she gets RM90 back.")
    void aSmallerRosterAcceptedDropsThePriceWithTheDiscountKeptAndTheDifferenceIsRefunded() {
        RosterProposal offer = reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        assertThat(offer.helpers()).isEqualTo(1);
        assertThat(offer.quote().hoursFee()).isEqualTo(Money.myr(90));
        assertThat(offer.quote().travelFee()).isEqualTo(Money.myr(20));
        assertThat(offer.quote().total()).isEqualTo(Money.myr(110));
        assertThat(offer.quote().baseCancellationFee()).isEqualTo(Money.myr("22.5"));
        assertThat(offer.quote().discountRate()).isEqualTo(Percentage.of(10));
        assertThat(offer.refund()).isEqualTo(Money.myr(90));
        assertThat(offer.settlement()).isEqualTo(SITI_ALONE_ON_THE_STANDARD_RESERVATION);
        assertThat(offer.proposedAt()).isEqualTo(thursday(9, 0));
        assertThat(offer.roster().members()).containsExactly(siti().asRosterMember());
        assertThat(reservation.proposal()).contains(offer);
        assertThat(reservation.currentQuote().total()).as("nothing changes until Aisyah accepts").isEqualTo(Money.myr(200));

        RosterAdjustment accepted = reservation.acceptProposal(List.of(siti()), friday(10, 0));

        assertThat(accepted).isEqualTo(new RosterAdjustment(1, offer.quote(), offer.settlement(), friday(10, 0)));
        assertThat(accepted.refund()).isEqualTo(Money.myr(90));
        assertThat(reservation.adjustment()).contains(accepted);
        assertThat(reservation.proposal()).isEmpty();
        assertThat(reservation.helpers()).isEqualTo(1);
        assertThat(reservation.currentQuote().total()).isEqualTo(Money.myr(110));
        assertThat(reservation.quote().total()).as("it keeps the price it was booked at").isEqualTo(Money.myr(200));
        assertThat(reservation.booking().helpers()).isEqualTo(2);
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("There is no fine for a smaller roster: Kemas Home isn't fined and Aisyah gets no compensation, only the difference.")
    void aSmallerRosterAcceptedHasNoFineAndNoCompensation() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        Settlement settlement = reservation.acceptProposal(List.of(siti()), friday(10, 0)).settlement();

        assertThat(settlement.providerFine()).isEqualTo(Money.myr(0));
        assertThat(settlement.compensation()).isEqualTo(Money.myr(0));
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(90));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti with Farid: a supervisor is extra and isn't billed, so the price is the same RM110.")
    void aSupervisorOnTheSmallerRosterIsNotBilled() {
        RosterProposal offer = reservation.proposeSmallerRoster(List.of(siti(), farid()), thursday(9, 0));

        assertThat(offer.helpers()).isEqualTo(1);
        assertThat(offer.quote().total()).isEqualTo(Money.myr(110));
        assertThat(reservation.proposedMemberIds()).containsExactly(SITI_MEMBER_ID, farid().memberId());
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("At 9:44 Kemas Home can still offer a smaller roster and Aisyah can still accept it; at 9:45 the roster is locked.")
    void theOfferAndTheAnswerAreOpenUntilTheCheckInPeriodStarts() {
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(siti()), saturday(9, 45)))
                .isInstanceOf(RosterLockedException.class);
        assertThat(reservation.proposal()).isEmpty();

        reservation.proposeSmallerRoster(List.of(siti()), saturday(9, 44));
        assertThatThrownBy(() -> reservation.acceptProposal(List.of(siti()), saturday(9, 45)))
                .isInstanceOf(RosterLockedException.class);
        assertThatThrownBy(() -> reservation.declineProposal(saturday(9, 45)))
                .isInstanceOf(RosterLockedException.class);
        assertThat(reservation.proposal()).isPresent();

        reservation.acceptProposal(List.of(siti()), saturday(9, 44));

        assertThat(reservation.adjustment()).isPresent();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Had she declined, ROS-7 would apply: the offer is gone, the booking is as it was, and Kemas Home is fined RM69 at 9:45 if it still has no roster.")
    void aDeclinedOfferChangesNothingAndTheNoRosterPolicyAppliesAtCheckIn() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        reservation.declineProposal(friday(10, 0));

        assertThat(reservation.proposal()).isEmpty();
        assertThat(reservation.adjustment()).isEmpty();
        assertThat(reservation.helpers()).isEqualTo(2);
        assertThat(reservation.currentQuote().total()).isEqualTo(Money.myr(200));
        assertThat(reservation.roster()).isEmpty();

        Cancellation cancelled = reservation.lockRosterAtCheckIn(saturday(9, 45)).orElseThrow();

        assertThat(cancelled.cause()).isEqualTo(CancellationCause.NO_ROSTER);
        assertThat(cancelled.settlement().providerFine()).isEqualTo(Money.myr(69));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("After a decline Kemas Home can still find a second helper and roster Siti and Rina before 9:45, and no fine applies.")
    void afterADeclineTheProviderCanStillCompleteTheRoster() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.declineProposal(friday(10, 0));

        reservation.assignRoster(List.of(siti(), rina()), saturday(8, 0));

        assertThat(reservation.lockRosterAtCheckIn(saturday(9, 45))).isEmpty();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Aisyah doesn't answer by 9:45: the offer lapses with the cancellation, and the No Roster Policy applies.")
    void anOfferNobodyAnsweredLapsesAtCheckInAndTheNoRosterPolicyApplies() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        Cancellation cancelled = reservation.lockRosterAtCheckIn(saturday(9, 45)).orElseThrow();

        assertThat(cancelled.cause()).isEqualTo(CancellationCause.NO_ROSTER);
        assertThat(cancelled.settlement().customerRefund().plus(cancelled.settlement().compensation()))
                .isEqualTo(Money.myr(208));
        assertThat(reservation.proposal()).isEmpty();
        assertThat(reservation.helpers()).isEqualTo(2);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers a smaller roster but then completes the full roster itself: the offer lapses with it, and Aisyah can't accept it.")
    void aCompleteRosterEndsTheOffer() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        reservation.assignRoster(List.of(siti(), nurul()), friday(9, 0));

        assertThat(reservation.proposal()).isEmpty();
        assertThatThrownBy(() -> reservation.acceptProposal(List.of(siti()), friday(10, 0)))
                .isInstanceOf(NoSmallerRosterProposedException.class);
        assertThat(reservation.lockRosterAtCheckIn(saturday(9, 45))).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("An offer nobody answered lapses at 9:45 even when the roster was complete, and the roster locks as it was.")
    void aCompleteRosterLocksAndDropsAnOfferNobodyAnswered() {
        reservation.assignRoster(List.of(siti(), nurul()), thursday(9, 0));
        reservation.proposeSmallerRoster(List.of(siti()), thursday(10, 0));

        assertThat(reservation.lockRosterAtCheckIn(saturday(9, 45))).isEmpty();

        assertThat(reservation.proposal()).isEmpty();
        assertThat(reservation.helpers()).isEqualTo(2);
        assertThat(memberIds()).containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("A smaller roster has at least one helper and fewer than the 2 booked: Siti and Nurul, or only Farid, isn't smaller.")
    void anOfferMustBeSmallerThanTheReservationAndHaveAHelper() {
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(siti(), nurul()), thursday(9, 0)))
                .isInstanceOf(NotASmallerRosterException.class);
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(farid()), thursday(9, 0)))
                .isInstanceOf(NotASmallerRosterException.class);
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(), thursday(9, 0)))
                .isInstanceOf(NotASmallerRosterException.class);
        assertThat(reservation.proposal()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Siti can't be offered twice, and every member offered must be able to work on the job.")
    void everyMemberOfferedMustBeRosterableAndOnlyOnce() {
        Reservation booked = threeHelpers();
        assertThatThrownBy(() -> booked.proposeSmallerRoster(List.of(siti(), siti()), thursday(9, 0)))
                .isInstanceOf(IncompleteRosterException.class);
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(deactivated(siti())), thursday(9, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
        assertThat(reservation.proposal()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("One offer waits for Aisyah at a time; once she declines, Kemas Home can make another.")
    void anotherOfferWaitsUntilTheCustomerAnswersTheFirst() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(nurul()), thursday(10, 0)))
                .isInstanceOf(SmallerRosterAlreadyProposedException.class);
        assertThat(reservation.proposedMemberIds()).containsExactly(SITI_MEMBER_ID);

        reservation.declineProposal(thursday(11, 0));
        reservation.proposeSmallerRoster(List.of(nurul()), thursday(12, 0));

        assertThat(reservation.proposedMemberIds()).containsExactly(NURUL_MEMBER_ID);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("With no offer waiting, Aisyah has nothing to accept or decline; and no offer is waiting once accepted or declined.")
    void thereIsNothingToAnswerWithoutAnOffer() {
        assertThat(reservation.proposedMemberIds()).isEmpty();
        assertThatThrownBy(() -> reservation.acceptProposal(List.of(siti()), thursday(9, 0)))
                .isInstanceOf(NoSmallerRosterProposedException.class);
        assertThatThrownBy(() -> reservation.declineProposal(thursday(9, 0)))
                .isInstanceOf(NoSmallerRosterProposedException.class);

        reservation.proposeSmallerRoster(List.of(siti()), thursday(10, 0));
        reservation.acceptProposal(List.of(siti()), thursday(11, 0));

        assertThatThrownBy(() -> reservation.acceptProposal(List.of(siti()), thursday(12, 0)))
                .isInstanceOf(NoSmallerRosterProposedException.class);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Siti is deactivated before Aisyah accepts: she can't accept Siti alone, and Kemas Home has to offer again.")
    void anOfferWithAMemberWhoIsNoLongerRosterableCannotBeAccepted() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        assertThatThrownBy(() -> reservation.acceptProposal(List.of(deactivated(siti())), thursday(10, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
        assertThatThrownBy(() -> reservation.acceptProposal(List.of(nurul()), thursday(10, 0)))
                .as("the member offered is missing").isInstanceOf(MemberNotRosterableException.class);
        assertThat(reservation.proposal()).isPresent();
        assertThat(reservation.adjustment()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("A request nobody accepted yet, or a cancelled reservation, has no roster to make smaller.")
    void onlyAReservedReservationCanHaveASmallerRoster() {
        Reservation requested = Marketplace.withKemasHome().requestStandard();
        assertThatThrownBy(() -> requested.proposeSmallerRoster(List.of(siti()), thursday(9, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);

        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.cancelByCustomer(friday(20, 0));

        assertThat(reservation.proposal()).as("the offer ends with the reservation").isEmpty();
        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(siti()), friday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> reservation.acceptProposal(List.of(siti()), friday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> reservation.declineProposal(friday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Once Aisyah accepts Siti alone, the roster needs 1 helper: Siti and Rina is too many, and Rina alone is right.")
    void afterAnAcceptedSmallerRosterTheRosterNeedsTheNewNumberOfHelpers() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.acceptProposal(List.of(siti()), friday(10, 0));

        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), rina()), friday(11, 0)))
                .isInstanceOf(IncompleteRosterException.class);

        reservation.assignRoster(List.of(rina()), friday(12, 0));

        assertThat(memberIds()).containsExactly(RINA_MEMBER_ID);
        assertThat(reservation.helpers()).isEqualTo(1);
        assertThat(reservation.lockRosterAtCheckIn(saturday(9, 45))).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home can offer smaller again: from 3 helpers (RM290) to 2 (RM200, RM90 back) and then to 1 (RM110, RM90 back), the discount kept each time.")
    void aSmallerRosterCanBeMadeSmallerAgainFromTheCurrentPrice() {
        Reservation booked = threeHelpers();
        assertThat(booked.quote().total()).isEqualTo(Money.myr(290));

        RosterProposal toTwo = booked.proposeSmallerRoster(List.of(siti(), nurul()), thursday(9, 0));
        RosterAdjustment first = booked.acceptProposal(List.of(siti(), nurul()), thursday(10, 0));

        assertThat(toTwo.quote().total()).isEqualTo(Money.myr(200));
        assertThat(first.refund()).isEqualTo(Money.myr(90));
        assertThat(booked.helpers()).isEqualTo(2);

        RosterProposal toOne = booked.proposeSmallerRoster(List.of(siti()), thursday(11, 0));
        RosterAdjustment second = booked.acceptProposal(List.of(siti()), thursday(12, 0));

        assertThat(toOne.quote().total()).isEqualTo(Money.myr(110));
        assertThat(second.refund()).isEqualTo(Money.myr(90));
        assertThat(second.settlement().customerFee()).isEqualTo(Money.myr(110));
        assertThat(booked.currentQuote().total()).isEqualTo(Money.myr(110));
        assertThat(booked.quote().total()).isEqualTo(Money.myr(290));
        assertThat(booked.adjustment()).contains(second);
        assertThatThrownBy(() -> booked.proposeSmallerRoster(List.of(siti()), thursday(13, 0)))
                .as("one helper is the smallest roster").isInstanceOf(NotASmallerRosterException.class);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Aisyah booked 2 hours with 2 helpers, which earns no discount: Siti alone is 25 × 2 × 1 + 20 = RM70, and the discount is not newly given.")
    void aBookingWithoutADiscountKeepsPayingFullRate() {
        Reservation twoHours = Marketplace.withKemasHome()
                .book(aisyahBooking(JOB_START, Duration.ofHours(2), 2), TAPPED_PAY_AT, REQUESTED_AT);
        twoHours.accept(ACCEPTED_AT);

        RosterProposal offer = twoHours.proposeSmallerRoster(List.of(siti()), thursday(9, 0));

        assertThat(twoHours.quote().total()).isEqualTo(Money.myr(120));
        assertThat(offer.quote().total()).isEqualTo(Money.myr(70));
        assertThat(offer.quote().discountRate()).isEqualTo(Percentage.ZERO);
        assertThat(offer.refund()).isEqualTo(Money.myr(50));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("After Siti alone is accepted, a Friday 22:00 cancellation by Aisyah costs the Base Cancellation Fee for 1 helper, RM22.50, of the RM110 she still pays.")
    void laterSettlementsWorkFromThePriceAfterTheSmallerRoster() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.acceptProposal(List.of(siti()), thursday(10, 0));

        Settlement settlement = reservation.cancelByCustomer(friday(22, 0)).settlement();

        assertThat(settlement.customerFee()).isEqualTo(Money.myr("22.5"));
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr("87.5"));
        assertThat(settlement.customerFee().plus(settlement.customerRefund())).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("NRS-2")
    @Tag("ROS-8")
    @DisplayName("Siti alone is accepted and then she isn't active at 9:45: the No Roster Policy works from RM110, fine 22.50 + 12% of 110 = RM35.70, Aisyah gets RM110 + RM4.40.")
    void theNoRosterPolicyAfterASmallerRosterWorksFromTheNewPrice() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.acceptProposal(List.of(siti()), thursday(10, 0));
        reservation.rosterMemberStoppedBeingActive(SITI_MEMBER_ID, friday(10, 0));

        Settlement settlement = reservation.lockRosterAtCheckIn(saturday(9, 45)).orElseThrow().settlement();

        assertThat(settlement.providerFine()).isEqualTo(Money.myr("35.7"));
        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(110));
        assertThat(settlement.compensation()).isEqualTo(Money.myr("4.4"));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("The roster Siti alone is locked at 9:45 and the job goes on with one helper at RM110.")
    void aSmallerRosterLocksAtCheckInLikeAnyOther() {
        reservation.proposeSmallerRoster(List.of(siti()), thursday(9, 0));
        reservation.acceptProposal(List.of(siti()), thursday(10, 0));

        assertThat(reservation.lockRosterAtCheckIn(saturday(9, 45))).isEmpty();
        assertThat(reservation.roster().orElseThrow().isLocked()).isTrue();
        assertThat(reservation.currentQuote().total()).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Members offered who aren't Kemas Home's own can't be offered to Aisyah.")
    void anotherProvidersMemberCannotBeOffered() {
        RosterableMember stranger = RosterableMember.local("member-stranger", "person-stranger", "provider-another",
                com.homa.reservation.domain.roster.MemberRole.HELPER, "Stranger", "photo-stranger", true);

        assertThatThrownBy(() -> reservation.proposeSmallerRoster(List.of(stranger), thursday(9, 0)))
                .isInstanceOf(MemberNotRosterableException.class);
    }
}
