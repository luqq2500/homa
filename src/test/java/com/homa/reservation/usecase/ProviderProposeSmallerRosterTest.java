package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.RINA_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.SITI_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.SmallerRosterProposedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.exception.NotASmallerRosterException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.exception.SmallerRosterAlreadyProposedException;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterRequest;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterResponse;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.TeamMemberNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderProposeSmallerRosterTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private ProposeSmallerRosterResponse propose(String personId, String... memberIds) {
        return bed.proposeSmallerRoster
                .propose(new ProposeSmallerRosterRequest(reserved.id().value(), personId, List.of(memberIds)));
    }

    private void assertNoOffer() {
        assertThat(bed.reload(reserved).proposal()).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti alone: Aisyah is told it costs RM110 and she'd get RM90 back, and has until 9:45 to answer.")
    void providerOffersASmallerRosterAndTheCustomerIsTold() {
        bed.publisher.clear();
        bed.time.set(friday(9, 0));

        ProposeSmallerRosterResponse offered = propose(AZMAN_PERSON_ID, SITI_MEMBER_ID);

        String id = reserved.id().value();
        assertThat(offered).isEqualTo(new ProposeSmallerRosterResponse(id, 1, Money.myr(110), Money.myr(90),
                friday(9, 0), saturday(9, 45)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.proposedMemberIds()).containsExactly(SITI_MEMBER_ID);
        assertThat(saved.currentQuote().total()).as("nothing is refunded until she accepts").isEqualTo(Money.myr(200));
        assertThat(bed.publisher.single(SmallerRosterProposedIE.class)).isEqualTo(new SmallerRosterProposedIE(
                "reservation-event-4", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, List.of(SITI_MEMBER_ID), 1,
                Money.myr(110), Money.myr(90), friday(9, 0), saturday(9, 45), friday(9, 0)));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti with supervisor Farid: both are shown to Aisyah, and the price is still RM110.")
    void theOfferShowsEveryMemberOfferedAndTheEventCarriesThemInOrder() {
        bed.publisher.clear();

        propose(AZMAN_PERSON_ID, SITI_MEMBER_ID, FARID_MEMBER_ID);

        SmallerRosterProposedIE offered = bed.publisher.single(SmallerRosterProposedIE.class);
        assertThat(offered.memberIds()).containsExactly(SITI_MEMBER_ID, FARID_MEMBER_ID);
        assertThat(offered.helpers()).isEqualTo(1);
        assertThat(offered.newTotal()).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("The offer's member list can't be changed after it is published.")
    void thePublishedOfferIsFrozen() {
        List<String> members = new ArrayList<>(List.of(SITI_MEMBER_ID));
        SmallerRosterProposedIE offered = new SmallerRosterProposedIE("event-1", "reservation-1", AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, members, 1, Money.myr(110), Money.myr(90), friday(9, 0), saturday(9, 45),
                friday(9, 0));

        members.add(NURUL_MEMBER_ID);

        assertThat(offered.memberIds()).containsExactly(SITI_MEMBER_ID);
        assertThatThrownBy(() -> offered.memberIds().add(NURUL_MEMBER_ID))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Siti, rostered on Saturday, offers Nurul's replacement for Kemas Home with her own sign-in; Rina, not rostered, can't, and neither can Aisyah.")
    void aRosteredMemberOffersForTheProviderAndAnotherMemberCannot() {
        bed.time.set(friday(8, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        bed.publisher.clear();

        assertThatThrownBy(() -> propose(RINA_PERSON_ID, SITI_MEMBER_ID))
                .isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> propose(AISYAH_CUSTOMER_ID, SITI_MEMBER_ID))
                .isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> propose(KEMAS_HOME_PROVIDER_ID, SITI_MEMBER_ID))
                .isInstanceOf(NotReservationPartyException.class);
        assertNoOffer();

        propose(SITI_PERSON_ID, SITI_MEMBER_ID);

        assertThat(bed.reload(reserved).proposedMemberIds()).containsExactly(SITI_MEMBER_ID);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("At 9:44 Kemas Home can still offer a smaller roster; at 9:45 the roster is locked and nothing is offered.")
    void theOfferIsRefusedFromTheCheckInPeriod() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        assertThatThrownBy(() -> propose(AZMAN_PERSON_ID, SITI_MEMBER_ID)).isInstanceOf(RosterLockedException.class);
        assertNoOffer();

        bed.time.set(saturday(9, 44));
        propose(AZMAN_PERSON_ID, SITI_MEMBER_ID);

        assertThat(bed.publisher.single(SmallerRosterProposedIE.class).answerBy()).isEqualTo(saturday(9, 45));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Offering both helpers, or one Reservation hasn't heard of, is refused and nothing is offered.")
    void anOfferThatIsNotSmallerOrNamesAnUnknownMemberIsRefused() {
        bed.publisher.clear();

        assertThatThrownBy(() -> propose(AZMAN_PERSON_ID, SITI_MEMBER_ID, NURUL_MEMBER_ID))
                .isInstanceOf(NotASmallerRosterException.class);
        assertThatThrownBy(() -> propose(AZMAN_PERSON_ID, SITI_MEMBER_ID, "member-unknown"))
                .isInstanceOf(TeamMemberNotFoundException.class);
        assertThatThrownBy(() -> bed.proposeSmallerRoster.propose(
                new ProposeSmallerRosterRequest("reservation-unknown", AZMAN_PERSON_ID, List.of(SITI_MEMBER_ID))))
                .isInstanceOf(ReservationNotFoundException.class);
        assertNoOffer();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Offering again while Aisyah hasn't answered is refused, and her offer stays as it was shown to her.")
    void aSecondOfferIsRefusedWhileOneWaits() {
        propose(AZMAN_PERSON_ID, SITI_MEMBER_ID);
        bed.publisher.clear();

        assertThatThrownBy(() -> propose(AZMAN_PERSON_ID, NURUL_MEMBER_ID))
                .isInstanceOf(SmallerRosterAlreadyProposedException.class);

        assertThat(bed.reload(reserved).proposedMemberIds()).containsExactly(SITI_MEMBER_ID);
        assertThat(bed.publisher.published()).isEmpty();
    }
}
