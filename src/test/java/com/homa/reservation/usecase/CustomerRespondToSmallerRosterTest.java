package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.CLEAN_KITCHEN_PHOTO;
import static com.homa.reservation.usecase.ReservationTestBed.NURUL_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.SITI_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.RosterChangedIE;
import com.homa.ie.events.SettlementAdjustedIE;
import com.homa.ie.events.SmallerRosterDeclinedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.NoSmallerRosterProposedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.ListMyReservationsRequest;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInRequest;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterRequest;
import com.homa.reservation.usecase.dto.RespondToSmallerRosterRequest;
import com.homa.reservation.usecase.dto.RespondToSmallerRosterResponse;
import com.homa.reservation.usecase.dto.ViewReservationRequest;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerRespondToSmallerRosterTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();
    private final String id = reserved.id().value();

    private void kemasHomeOffersSitiAlone(Instant at) {
        bed.time.set(at);
        bed.proposeSmallerRoster.propose(new ProposeSmallerRosterRequest(id, AZMAN_PERSON_ID, List.of(SITI_MEMBER_ID)));
        bed.publisher.clear();
    }

    private RespondToSmallerRosterResponse answer(String customerId, boolean accept) {
        return bed.respondToSmallerRoster.respond(new RespondToSmallerRosterRequest(id, customerId, accept));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti alone and Aisyah accepts: RM110 for the job, RM90 back, and Payment is told the new total and the refund.")
    void acceptingTheOfferRefundsTheDifferenceAndTheReservationGoesOnWithOneHelper() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));

        RespondToSmallerRosterResponse answered = answer(AISYAH_CUSTOMER_ID, true);

        assertThat(answered).isEqualTo(new RespondToSmallerRosterResponse(id, true, 1, Money.myr(110), Money.myr(90),
                friday(10, 0)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.helpers()).isEqualTo(1);
        assertThat(saved.currentQuote().total()).isEqualTo(Money.myr(110));
        assertThat(saved.proposal()).isEmpty();
        assertThat(saved.roster().orElseThrow().members()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).hasSize(2);
        assertThat(bed.publisher.published().get(0)).isEqualTo(
                new RosterChangedIE("reservation-event-5", id, List.of(SITI_MEMBER_ID), friday(10, 0)));
        assertThat(bed.publisher.published().get(1)).isEqualTo(new SettlementAdjustedIE("reservation-event-6", id,
                AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, "adjustment-" + id + "-1", "SMALLER_ROSTER",
                Money.myr(90), Money.myr(110), friday(10, 0), friday(10, 0)));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Had she declined, ROS-7 would apply: nothing is refunded, the booking is as it was, and Kemas Home is told it can still complete the roster by 9:45.")
    void decliningTheOfferChangesNothingAndTheProviderIsTold() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));

        RespondToSmallerRosterResponse answered = answer(AISYAH_CUSTOMER_ID, false);

        assertThat(answered).isEqualTo(new RespondToSmallerRosterResponse(id, false, 2, Money.myr(200), Money.myr(0),
                friday(10, 0)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.proposal()).isEmpty();
        assertThat(saved.adjustment()).isEmpty();
        assertThat(saved.helpers()).isEqualTo(2);
        assertThat(saved.roster()).isEmpty();
        assertThat(bed.publisher.single(SmallerRosterDeclinedIE.class)).isEqualTo(new SmallerRosterDeclinedIE(
                "reservation-event-5", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, friday(10, 0),
                saturday(9, 45), friday(10, 0)));
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Aisyah declined and Kemas Home found no one else: at 9:45 the No Roster Policy applies, fine RM69 and RM208 for Aisyah.")
    void aDeclinedOfferLeavesTheNoRosterPolicyToApplyAtCheckIn() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        answer(AISYAH_CUSTOMER_ID, false);
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        bed.lockRostersAtCheckIn.lockRosters(new LockRostersAtCheckInRequest());

        ReservationCancelledIE cancelled = bed.publisher.single(ReservationCancelledIE.class);
        assertThat(cancelled.cause()).isEqualTo("NO_ROSTER");
        assertThat(cancelled.providerFine()).isEqualTo(Money.myr(69));
        assertThat(cancelled.customerRefund().plus(cancelled.compensation())).isEqualTo(Money.myr(208));
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Aisyah doesn't answer by 9:45: the offer lapses, the No Roster Policy applies, and answering at 9:46 is too late.")
    void anOfferNobodyAnsweredLapsesAndALateAnswerIsRefused() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(saturday(9, 45));
        bed.lockRostersAtCheckIn.lockRosters(new LockRostersAtCheckInRequest());
        bed.publisher.clear();
        bed.time.set(saturday(9, 46));

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, false))
                .isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.reload(reserved).helpers()).isEqualTo(2);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("At 9:44 Aisyah can still accept; at 9:45 the roster is locked and she can neither accept nor decline.")
    void theAnswerIsRefusedFromTheCheckInPeriod() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(saturday(9, 45));

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true)).isInstanceOf(RosterLockedException.class);
        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, false)).isInstanceOf(RosterLockedException.class);
        assertThat(bed.reload(reserved).proposal()).isPresent();
        assertThat(bed.publisher.published()).isEmpty();

        bed.time.set(saturday(9, 44));
        answer(AISYAH_CUSTOMER_ID, true);

        assertThat(bed.reload(reserved).helpers()).isEqualTo(1);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Only Aisyah answers her offer: Azman, Siti and a stranger can't.")
    void onlyTheReservationsCustomerAnswers() {
        kemasHomeOffersSitiAlone(friday(9, 0));

        for (String actor : List.of(AZMAN_PERSON_ID, SITI_PERSON_ID, KEMAS_HOME_PROVIDER_ID, "customer-mei")) {
            assertThatThrownBy(() -> answer(actor, true)).isInstanceOf(NotReservationPartyException.class);
            assertThatThrownBy(() -> answer(actor, false)).isInstanceOf(NotReservationPartyException.class);
        }
        assertThatThrownBy(() -> bed.respondToSmallerRoster
                .respond(new RespondToSmallerRosterRequest("reservation-unknown", AISYAH_CUSTOMER_ID, true)))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThat(bed.reload(reserved).proposal()).isPresent();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("With no offer waiting there is nothing to answer.")
    void anAnswerWithoutAnOfferIsRefused() {
        bed.publisher.clear();

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true)).isInstanceOf(NoSmallerRosterProposedException.class);
        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, false)).isInstanceOf(NoSmallerRosterProposedException.class);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("A redelivered answer changes nothing and refunds nothing twice: Aisyah's tap on Accept, sent again, is refused and publishes nothing.")
    void anAnswerGivenTwiceIsRefusedTheSecondTime() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));
        answer(AISYAH_CUSTOMER_ID, true);
        bed.publisher.clear();

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true)).isInstanceOf(NoSmallerRosterProposedException.class);
        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, false)).isInstanceOf(NoSmallerRosterProposedException.class);

        assertThat(bed.reload(reserved).currentQuote().total()).isEqualTo(Money.myr(110));
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Siti is deactivated while the offer waits: Aisyah can't accept Siti alone, and the offer stays for Kemas Home to replace.")
    void anOfferWithAMemberNoLongerActiveCannotBeAccepted() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.teamMemberDeactivated.handle(bed.deactivation(SITI_MEMBER_ID, SITI_PERSON_ID, "DEACTIVATED"));
        bed.publisher.clear();
        bed.time.set(friday(10, 0));

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true)).isInstanceOf(MemberNotRosterableException.class);

        assertThat(bed.reload(reserved).helpers()).isEqualTo(2);
        assertThat(bed.reload(reserved).proposal()).isPresent();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("When Kemas Home completes the full roster while Aisyah accepts, whoever is saved first wins: the offer is gone, and she is told so.")
    void aCompleteRosterSavedFirstEndsTheOfferBeforeItIsAccepted() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));
        bed.reservations.beforeNextSave(() -> bed.assignRoster.assign(
                new AssignRosterRequest(id, AZMAN_PERSON_ID, List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID))));
        bed.publisher.clear();

        assertThatThrownBy(() -> answer(AISYAH_CUSTOMER_ID, true)).isInstanceOf(NoSmallerRosterProposedException.class);

        assertThat(bed.reload(reserved).helpers()).isEqualTo(2);
        assertThat(bed.reload(reserved).roster().orElseThrow().members()).hasSize(2);
        assertThat(bed.publisher.published()).hasSize(1);
        assertThat(bed.publisher.published().get(0)).isInstanceOf(RosterChangedIE.class);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("After Aisyah accepts, her view and list show 1 helper and RM110, and Kemas Home's completion carries RM110 as the payout gross.")
    void everythingAfterTheAcceptanceShowsTheNewPriceAndHelpers() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));
        answer(AISYAH_CUSTOMER_ID, true);

        var view = bed.viewReservation.view(new ViewReservationRequest(id, AISYAH_CUSTOMER_ID));
        var listed = bed.listMyReservations.list(new ListMyReservationsRequest(AISYAH_CUSTOMER_ID)).reservations().get(0);

        assertThat(view.helpers()).isEqualTo(1);
        assertThat(view.hoursFee()).isEqualTo(Money.myr(90));
        assertThat(view.travelFee()).isEqualTo(Money.myr(20));
        assertThat(view.total()).isEqualTo(Money.myr(110));
        assertThat(listed.helpers()).isEqualTo(1);
        assertThat(listed.total()).isEqualTo(Money.myr(110));

        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));
        bed.publisher.clear();
        bed.completeReservation.complete(new CompleteReservationRequest(id, SITI_PERSON_ID, List.of(CLEAN_KITCHEN_PHOTO)));

        assertThat(bed.publisher.single(ReservationCompletedIE.class).payoutGross()).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Aisyah accepts and then cancels on Friday 22:00 under Standard: she pays the Base Cancellation Fee for one helper, RM22.50, and gets RM87.50 back.")
    void aLaterCancellationSettlesFromTheNewPrice() {
        kemasHomeOffersSitiAlone(friday(9, 0));
        bed.time.set(friday(10, 0));
        answer(AISYAH_CUSTOMER_ID, true);
        bed.publisher.clear();
        bed.time.set(friday(22, 0));

        bed.customerCancel.cancel(new com.homa.reservation.usecase.dto.CustomerCancelReservationRequest(id,
                AISYAH_CUSTOMER_ID));

        ReservationCancelledIE cancelled = bed.publisher.single(ReservationCancelledIE.class);
        assertThat(cancelled.customerFee()).isEqualTo(Money.myr("22.5"));
        assertThat(cancelled.customerRefund()).isEqualTo(Money.myr("87.5"));
    }
}
