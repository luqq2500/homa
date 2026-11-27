package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.HOURS_FEE;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.CompletionPhotoRequiredException;
import com.homa.reservation.domain.exception.JobNotEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationCompletionTest {

    private static final CompletionPhoto CLEAN_KITCHEN = new CompletionPhoto("photo-clean-kitchen");

    private final Reservation reservation = Marketplace.withKemasHome().reserveStandard();

    @Test
    @Tag("JOB-2")
    @DisplayName("The team finishes at 13:30. Aisyah pays for 4 hours.")
    void billingIsForTheBookedHoursEvenWhenTheTeamFinishesEarly() {
        reservation.markCompleted(List.of(CLEAN_KITCHEN), JOB_END);

        assertThat(reservation.quote().hoursFee()).isEqualTo(HOURS_FEE);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("The button appears at 14:00. Siti uploads a photo of the kitchen and marks the job Completed.")
    void providerMarksTheJobCompletedWithAnAfterPhotoOnlyAfterTheJobEnds() {
        assertThatThrownBy(() -> reservation.markCompleted(List.of(CLEAN_KITCHEN), saturday(13, 30)))
                .isInstanceOf(JobNotEndedException.class);

        reservation.markCompleted(List.of(CLEAN_KITCHEN), saturday(14, 0));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("At 13:59 the job hasn't ended; without an after-photo it can't be marked Completed; a request not yet Reserved can't be either.")
    void completionIsRefusedBeforeTheEndWithoutAPhotoOrBeforeReserved() {
        assertThatThrownBy(() -> reservation.markCompleted(List.of(CLEAN_KITCHEN), saturday(13, 59)))
                .isInstanceOf(JobNotEndedException.class);
        assertThatThrownBy(() -> reservation.markCompleted(List.of(), JOB_END))
                .isInstanceOf(CompletionPhotoRequiredException.class);
        Reservation requested = Marketplace.withKemasHome().requestStandard();
        assertThatThrownBy(() -> requested.markCompleted(List.of(CLEAN_KITCHEN), JOB_END))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("JOB-4")
    @DisplayName("Aisyah gets a notification at 14:01 with the after-photo.")
    void completedJobCarriesWhenItWasMarkedAndItsAfterPhoto() {
        reservation.markCompleted(List.of(CLEAN_KITCHEN), saturday(14, 1));

        assertThat(reservation.completedAt()).contains(saturday(14, 1));
        assertThat(reservation.completionPhotos()).containsExactly(CLEAN_KITCHEN);
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("Kemas Home is reminded at 14:00 and 16:00, and marks it on Monday.")
    void uncompletedJobAwaitsCompletionFromItsEndUntilMarked() {
        Instant monday = saturday(10, 0).plusSeconds(2 * 86_400);

        assertThat(reservation.awaitsCompletionAt(saturday(13, 59))).isFalse();
        assertThat(reservation.awaitsCompletionAt(saturday(14, 0))).isTrue();
        assertThat(reservation.awaitsCompletionAt(saturday(16, 0))).isTrue();
        assertThat(reservation.completionReminders()).containsExactly(saturday(14, 0), saturday(16, 0));

        reservation.markCompleted(List.of(CLEAN_KITCHEN), monday);

        assertThat(reservation.awaitsCompletionAt(monday)).isFalse();
        assertThat(Marketplace.withKemasHome().requestStandard().awaitsCompletionAt(saturday(14, 0))).isFalse();
    }

    @Test
    @Tag("JOB-6")
    @DisplayName("Support checks the after-photos when Aisyah reports the bathroom wasn't cleaned.")
    void completionPhotosAreKeptAsEvidence() {
        List<CompletionPhoto> uploaded = new ArrayList<>(List.of(CLEAN_KITCHEN, new CompletionPhoto("photo-bathroom")));
        reservation.markCompleted(uploaded, JOB_END);
        uploaded.clear();

        assertThat(reservation.completionPhotos()).extracting(CompletionPhoto::reference)
                .containsExactly("photo-clean-kitchen", "photo-bathroom");
    }

    @Test
    @Tag("JOB-1")
    @DisplayName("The reservation's phase follows its job time.")
    void reservationPhaseFollowsItsJobTime() {
        assertThat(reservation.phaseAt(tuesday(9, 0))).isEqualTo(ReservationPhase.BEFORE_CHECK_IN);
        assertThat(reservation.phaseAt(saturday(9, 50))).isEqualTo(ReservationPhase.CHECK_IN);
    }
}
