package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.CLEAN_KITCHEN_PHOTO;
import static com.homa.reservation.usecase.ReservationTestBed.NURUL_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.RINA_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.SITI_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.ProviderCancelReservationRequest;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * PRV-11: everyone signs in with their own account. The owner acts on every reservation of their provider, a member
 * only on the ones they are rostered on, and the provider's own id is no one's sign-in.
 */
class ProviderSideActorsTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private Reservation reservedAndRostered() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        bed.publisher.clear();
        return reserved;
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Siti, rostered on Saturday, uploads the after-photo and marks the job Completed. Azman never shares his sign-in.")
    void aRosteredMemberMarksTheJobCompletedWithTheirOwnSignIn() {
        Reservation reserved = reservedAndRostered();
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));

        bed.completeReservation.complete(
                new CompleteReservationRequest(reserved.id().value(), SITI_PERSON_ID, List.of(CLEAN_KITCHEN_PHOTO)));

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(bed.publisher.single(ReservationCompletedIE.class).completionPhotoRefs())
                .containsExactly(CLEAN_KITCHEN_PHOTO);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Rina isn't rostered on Saturday, so she can't mark its job Completed, and neither can the provider's own id, or Aisyah.")
    void aMemberNotRosteredOnTheReservationCannotMarkItCompleted() {
        Reservation reserved = reservedAndRostered();
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));

        for (String actor : List.of(RINA_PERSON_ID, KEMAS_HOME_PROVIDER_ID, AISYAH_CUSTOMER_ID)) {
            assertThatThrownBy(() -> bed.completeReservation.complete(new CompleteReservationRequest(
                    reserved.id().value(), actor, List.of(CLEAN_KITCHEN_PHOTO))))
                    .isInstanceOf(NotReservationPartyException.class);
        }

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Azman, who isn't rostered, still marks Saturday's job Completed: an owner acts on any reservation of Kemas Home.")
    void theOwnerMarksAnyJobOfTheirProviderCompletedWithoutBeingRostered() {
        Reservation reserved = reservedAndRostered();
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));

        bed.completeReservation.complete(
                new CompleteReservationRequest(reserved.id().value(), AZMAN_PERSON_ID, List.of(CLEAN_KITCHEN_PHOTO)));

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Siti, rostered with Nurul on Friday, swaps Nurul for Rina with her own sign-in; Rina, once rostered, can do the same.")
    void aRosteredMemberChangesTheRosterAndTheirReplacementTakesOver() {
        Reservation reserved = reservedAndRostered();
        String id = reserved.id().value();
        bed.time.set(friday(19, 0));

        bed.assignRoster.assign(new AssignRosterRequest(id, SITI_PERSON_ID, List.of(SITI_MEMBER_ID, RINA_MEMBER_ID)));

        assertThat(bed.reload(reserved).roster().orElseThrow().members()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID, RINA_MEMBER_ID);
        assertThatThrownBy(() -> bed.assignRoster.assign(
                new AssignRosterRequest(id, NURUL_PERSON_ID, List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID))))
                .isInstanceOf(NotReservationPartyException.class);
        bed.assignRoster.assign(new AssignRosterRequest(id, RINA_PERSON_ID, List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        assertThat(bed.reload(reserved).roster().orElseThrow().members()).extracting(RosterMember::memberId)
                .containsExactly(SITI_MEMBER_ID, NURUL_MEMBER_ID);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Nurul, rostered on Saturday, cancels it for Kemas Home with her own sign-in; Rina, not rostered, can't.")
    void aRosteredMemberCancelsForTheProviderAndAnotherMemberCannot() {
        Reservation reserved = reservedAndRostered();
        String id = reserved.id().value();
        bed.time.set(friday(20, 0));

        assertThatThrownBy(() -> bed.providerCancel.cancel(new ProviderCancelReservationRequest(id, RINA_PERSON_ID)))
                .isInstanceOf(NotReservationPartyException.class);
        assertThat(bed.publisher.published()).isEmpty();

        bed.providerCancel.cancel(new ProviderCancelReservationRequest(id, NURUL_PERSON_ID));

        assertThat(bed.publisher.single(ReservationCancelledIE.class).cause()).isEqualTo("PROVIDER_CANCELLED");
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Nobody is rostered on a request, so only Azman accepts it; Siti, on the team, and the provider's own id can't.")
    void onlyTheOwnerAcceptsARequestBecauseNoOneIsRosteredYet() {
        Reservation requested = bed.requestStandard();
        bed.time.set(ReservationTestBed.ACCEPTED_AT);
        bed.publisher.clear();

        for (String actor : List.of(SITI_PERSON_ID, KEMAS_HOME_PROVIDER_ID)) {
            assertThatThrownBy(() -> bed.acceptReservation.accept(
                    new AcceptReservationRequest(requested.id().value(), actor)))
                    .isInstanceOf(NotReservationPartyException.class);
        }
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);

        bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), AZMAN_PERSON_ID));

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Only Azman declines a request; Siti, on the team but not rostered, can't.")
    void onlyTheOwnerDeclinesARequest() {
        Reservation requested = bed.requestStandard();
        bed.time.set(ReservationTestBed.ACCEPTED_AT);
        bed.publisher.clear();

        assertThatThrownBy(() -> bed.declineReservation.decline(
                new DeclineReservationRequest(requested.id().value(), SITI_PERSON_ID)))
                .isInstanceOf(NotReservationPartyException.class);
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);

        bed.declineReservation.decline(new DeclineReservationRequest(requested.id().value(), AZMAN_PERSON_ID));

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.DECLINED);
    }
}
