package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.BASE_CANCELLATION_FEE;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.payment.domain.PaymentFixtures.BATHROOM_REPORT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOME;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_PAYOUT;
import static com.homa.payment.domain.PaymentFixtures.MARKED_COMPLETED_AT;
import static com.homa.payment.domain.PaymentFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_RESERVATION;
import static com.homa.payment.domain.PaymentFixtures.VASE_REPORT;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesCancellationFeePayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesJobPayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesPayoutAtReserved;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.PayoutNotAwaitingCompletionException;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredPayoutTest {

    private static Payout restored(Payout stored) {
        return stored(stored.kind(), stored.gross(), stored.completedAt().orElse(null), stored.openReports(),
                stored.status(), stored.released().orElse(null));
    }

    private static Payout stored(PayoutKind kind, Money gross, Instant completedAt, Set<ReportId> openReports,
            PayoutStatus status, PayoutRelease release) {
        return Payout.restore(KEMAS_HOMES_PAYOUT, STANDARD_RESERVATION, KEMAS_HOME, kind, gross, REQUESTED_AT,
                PROTECTION_WINDOW_END, completedAt, openReports, status, release);
    }

    private static void assertRefused(PayoutKind kind, Money gross, Instant completedAt, PayoutStatus status,
            PayoutRelease release) {
        assertThatThrownBy(() -> stored(kind, gross, completedAt, Set.of(), status, release))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored payout needs ");
    }

    private static PayoutRelease releasedJobPayout() {
        return kemasHomesJobPayout().release(PROTECTION_WINDOW_END);
    }

    @Test
    @Tag("RPT-6")
    @DisplayName("Persistence: Kemas Home's completed payout comes back with the vase report still open, so it waits until the report is resolved.")
    void aStoredPayoutKeepsItsOpenReports() {
        Payout payout = kemasHomesJobPayout();
        payout.reportOpened(VASE_REPORT);
        payout.reportOpened(BATHROOM_REPORT);
        payout.reportResolved(BATHROOM_REPORT);

        Payout stored = restored(payout);

        assertThat(stored).usingRecursiveComparison().isEqualTo(payout);
        assertThat(stored.openReports()).containsExactly(VASE_REPORT);
        assertThat(stored.releasableAt(PROTECTION_WINDOW_END)).isFalse();
        stored.reportResolved(VASE_REPORT);
        assertThat(stored.release(PROTECTION_WINDOW_END).afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("Persistence: a stored payout still awaiting completion is released only once the job is marked Completed.")
    void aStoredPayoutAwaitingCompletionStillWaits() {
        Payout atReserved = kemasHomesPayoutAtReserved();

        Payout stored = restored(atReserved);

        assertThat(stored).usingRecursiveComparison().isEqualTo(atReserved);
        assertThat(stored.releasableAt(PROTECTION_WINDOW_END)).isFalse();
        stored.markCompleted(MARKED_COMPLETED_AT);
        assertThat(stored.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-6")
    @DisplayName("Persistence: a stored fee payout pays the RM45 fee without a completion, and a partial refund can bring a payout to RM0.")
    void aStoredFeePayoutNeedsNoCompletion() {
        Payout fee = kemasHomesCancellationFeePayout();
        Payout refundedToNothing = kemasHomesPayoutAtReserved();
        refundedToNothing.reduceByRefund(TOTAL_RESERVATION_FEE);

        Payout stored = restored(fee);

        assertThat(stored).usingRecursiveComparison().isEqualTo(fee);
        assertThat(stored.gross()).isEqualTo(BASE_CANCELLATION_FEE);
        assertThatThrownBy(() -> stored.markCompleted(MARKED_COMPLETED_AT))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class);
        assertThat(stored.releasableAt(PROTECTION_WINDOW_END)).isTrue();
        assertThat(restored(refundedToNothing).gross()).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("PAY-3")
    @DisplayName("Persistence: a released payout comes back with its release lines, and a voided one stays void; neither changes again.")
    void aStoredReleasedOrVoidPayoutStaysSo() {
        Payout released = kemasHomesJobPayout();
        released.release(PROTECTION_WINDOW_END);
        Payout voided = kemasHomesPayoutAtReserved();
        voided.reportOpened(VASE_REPORT);
        voided.makeVoid();

        Payout storedRelease = restored(released);
        Payout storedVoid = restored(voided);

        assertThat(storedRelease).usingRecursiveComparison().isEqualTo(released);
        assertThat(storedVoid).usingRecursiveComparison().isEqualTo(voided);
        assertThat(storedRelease.released()).contains(released.released().orElseThrow());
        assertThatThrownBy(storedRelease::makeVoid).isInstanceOf(PayoutNotPendingException.class);
        assertThat(storedVoid.releasableAt(PROTECTION_WINDOW_END)).isFalse();
    }

    @Test
    @Tag("PAY-6")
    @DisplayName("Persistence: a stored payout needs a gross that isn't negative, release lines exactly once released, and no completion when it pays a fee.")
    void aStoredPayoutThatDoesNotFitTogetherIsRefused() {
        assertRefused(PayoutKind.JOB, Money.myr(-1), null, PayoutStatus.PENDING, null);
        assertRefused(PayoutKind.JOB, TOTAL_RESERVATION_FEE, MARKED_COMPLETED_AT, PayoutStatus.RELEASED, null);
        assertRefused(PayoutKind.JOB, TOTAL_RESERVATION_FEE, MARKED_COMPLETED_AT, PayoutStatus.PENDING,
                releasedJobPayout());
        assertRefused(PayoutKind.FEE, BASE_CANCELLATION_FEE, MARKED_COMPLETED_AT, PayoutStatus.PENDING, null);
        assertThatThrownBy(() -> stored(null, TOTAL_RESERVATION_FEE, null, Set.of(), PayoutStatus.PENDING, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> stored(PayoutKind.JOB, TOTAL_RESERVATION_FEE, null, Set.of(), null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> stored(PayoutKind.JOB, null, null, Set.of(), PayoutStatus.PENDING, null))
                .isInstanceOf(NullPointerException.class);
    }
}
