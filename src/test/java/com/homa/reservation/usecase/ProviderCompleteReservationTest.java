package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.CLEAN_KITCHEN_PHOTO;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationCompletedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.CompletionPhoto;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.JobNotEndedException;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.CompleteReservationResponse;
import com.homa.reservation.usecase.exception.CompletionPhotoNotFoundException;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderCompleteReservationTest {

    private static final String BATHROOM_PHOTO = "photo-bathroom";

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    @BeforeEach
    void uploadAfterPhotos() {
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.photoStore.upload(BATHROOM_PHOTO);
        bed.publisher.clear();
    }

    private CompleteReservationResponse complete(String actorId, String... photoRefs) {
        return bed.completeReservation.complete(
                new CompleteReservationRequest(reserved.id().value(), actorId, List.of(photoRefs)));
    }

    private void assertStillReserved(ThrowingCallable call, Class<? extends Throwable> refusal) {
        assertThatThrownBy(call).isInstanceOf(refusal);
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("The button appears at 14:00. Siti uploads a photo of the kitchen and marks the job Completed; both calendars open.")
    void providerMarksTheJobCompletedAndTheTimeOpens() {
        bed.time.set(saturday(14, 1));

        CompleteReservationResponse completed = complete(KEMAS_HOME_PROVIDER_ID, CLEAN_KITCHEN_PHOTO, BATHROOM_PHOTO);

        String id = reserved.id().value();
        assertThat(completed).isEqualTo(new CompleteReservationResponse(id, "COMPLETED", saturday(14, 1)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(saved.completionPhotos()).containsExactly(new CompletionPhoto(CLEAN_KITCHEN_PHOTO),
                new CompletionPhoto(BATHROOM_PHOTO));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCompletedIE.class)).isEqualTo(new ReservationCompletedIE(
                "reservation-event-4", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, Money.myr(200),
                sunday(14, 0), saturday(14, 1), REQUESTED_AT, List.of(CLEAN_KITCHEN_PHOTO, BATHROOM_PHOTO),
                saturday(14, 1)));
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("At 13:59 the job hasn't ended, so it can't be marked Completed and the time stays blocked.")
    void jobCannotBeCompletedBeforeItEnds() {
        bed.time.set(saturday(13, 59));

        assertStillReserved(() -> complete(KEMAS_HOME_PROVIDER_ID, CLEAN_KITCHEN_PHOTO), JobNotEndedException.class);
    }

    @Test
    @Tag("JOB-6")
    @DisplayName("Every after-photo must have been uploaded to the photo store, to be kept as evidence.")
    void photoNotUploadedIsRefused() {
        bed.time.set(JOB_END);

        assertStillReserved(() -> complete(KEMAS_HOME_PROVIDER_ID, CLEAN_KITCHEN_PHOTO, "photo-never-uploaded"),
                CompletionPhotoNotFoundException.class);
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("Only Kemas Home marks its jobs Completed; Aisyah can't.")
    void onlyTheReservationsProviderCompletes() {
        bed.time.set(JOB_END);

        assertStillReserved(() -> complete(AISYAH_CUSTOMER_ID, CLEAN_KITCHEN_PHOTO),
                NotReservationPartyException.class);
    }

    @Test
    @Tag("RES-9")
    @DisplayName("When another save moved a schedule on meanwhile, the completion reloads and is saved on the next try.")
    void completionIsRetriedAfterAConflict() {
        bed.time.set(JOB_END);
        bed.reservations.conflictOnNextSaves(1);

        complete(KEMAS_HOME_PROVIDER_ID, CLEAN_KITCHEN_PHOTO);

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCompletedIE.class).reservationId())
                .isEqualTo(reserved.id().value());
    }

    @Test
    @Tag("RES-9")
    @DisplayName("After three conflicting saves the completion fails as a whole and nothing changes.")
    void completionGivesUpAfterThreeConflicts() {
        bed.time.set(JOB_END);
        bed.reservations.conflictOnNextSaves(3);

        assertStillReserved(() -> complete(KEMAS_HOME_PROVIDER_ID, CLEAN_KITCHEN_PHOTO),
                ScheduleChangedException.class);
    }
}
