package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationRequest;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationResponse;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PreviewCustomerCancellationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private PreviewCustomerCancellationResponse preview(String actorId) {
        return bed.previewCancellation.preview(new PreviewCustomerCancellationRequest(reserved.id().value(), actorId));
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("PreviewCustomerCancellation shows a RM45 fee. The Standard cut-off was Fri 21:45 and there is no grace period.")
    void previewShowsTheFeeAndTheRefundWithoutCancelling() {
        bed.publisher.clear();
        bed.time.set(friday(22, 0));
        long versionBefore = bed.reload(reserved).version();

        PreviewCustomerCancellationResponse previewed = preview(AISYAH_CUSTOMER_ID);

        assertThat(previewed).isEqualTo(new PreviewCustomerCancellationResponse(reserved.id().value(),
                "CUSTOMER_CANCELLED", friday(22, 0), Money.myr(45), Money.myr(155)));
        Reservation unchanged = bed.reload(reserved);
        assertThat(unchanged.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(unchanged.cancellation()).isEmpty();
        assertThat(unchanged.version()).isEqualTo(versionBefore);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("Reserved Monday 22:00, Aisyah cancels at 23:30 for free.")
    void previewShowsNoFeeWithinTheGracePeriod() {
        bed.time.set(monday(23, 30));

        assertThat(preview(AISYAH_CUSTOMER_ID)).isEqualTo(new PreviewCustomerCancellationResponse(
                reserved.id().value(), "CUSTOMER_CANCELLED", monday(23, 30), Money.myr(0), Money.myr(200)));
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("Aisyah cancels at 9:50: she pays 45 + 0 + 20 = RM65 and gets RM135 back.")
    void previewShowsTheLastMinuteFeeFromTheCheckInPeriod() {
        bed.time.set(saturday(9, 50));

        assertThat(preview(AISYAH_CUSTOMER_ID)).isEqualTo(new PreviewCustomerCancellationResponse(
                reserved.id().value(), "CUSTOMER_LAST_MINUTE", saturday(9, 50), Money.myr(65), Money.myr(135)));
    }

    @Test
    @Tag("CCX-4")
    @DisplayName("Standard: free until Friday 21:45.")
    void theFeeFollowsTheMomentAskedAt() {
        bed.time.set(friday(21, 44));
        assertThat(preview(AISYAH_CUSTOMER_ID).customerFee()).isEqualTo(Money.myr(0));

        bed.time.set(friday(21, 45));
        assertThat(preview(AISYAH_CUSTOMER_ID).customerFee()).isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("Only Aisyah sees what cancelling her reservation would cost: not Kemas Home or Mei, and an unknown reservation is refused.")
    void onlyTheReservationsCustomerPreviews() {
        bed.time.set(friday(22, 0));

        assertThatThrownBy(() -> preview(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> preview(MEI_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.previewCancellation
                .preview(new PreviewCustomerCancellationRequest("reservation-unknown", AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @Tag("REQ-1")
    @DisplayName("A Requested reservation has no cancellation fee to preview: it is withdrawn for free instead.")
    void aRequestedReservationHasNothingToPreview() {
        ReservationTestBed other = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation requested = other.requestStandard();
        other.time.set(monday(21, 0));

        assertThatThrownBy(() -> other.previewCancellation
                .preview(new PreviewCustomerCancellationRequest(requested.id().value(), AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("Once the job has ended there is nothing to cancel, so there is no fee to preview.")
    void aJobThatEndedHasNothingToPreview() {
        bed.time.set(JOB_END);

        assertThatThrownBy(() -> preview(AISYAH_CUSTOMER_ID)).isInstanceOf(JobAlreadyEndedException.class);
    }
}
