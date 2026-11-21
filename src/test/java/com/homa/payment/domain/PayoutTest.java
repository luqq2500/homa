package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.domain.PaymentFixtures.BATHROOM_REPORT;
import static com.homa.payment.domain.PaymentFixtures.CANCELLED_AT;
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

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import com.homa.payment.domain.exception.PayoutNotAwaitingCompletionException;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import com.homa.payment.domain.exception.PayoutNotReleasableException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PayoutTest {

    private static final Instant ONE_MINUTE_BEFORE_WINDOW_ENDS = PROTECTION_WINDOW_END.minus(Duration.ofMinutes(1));

    @Test
    @Tag("RPT-6")
    @DisplayName("Kemas Home accepts on Monday 22:00: its RM200 payout opens for the reservation, waiting for the job "
            + "to be marked Completed.")
    void reservedReservationOpensAJobPayoutWaitingForCompletion() {
        Payout payout = kemasHomesPayoutAtReserved();

        assertThat(payout.id()).isEqualTo(KEMAS_HOMES_PAYOUT);
        assertThat(payout.reservationId()).isEqualTo(STANDARD_RESERVATION);
        assertThat(payout.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(payout.gross()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(payout.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(payout.protectionWindowEnd()).isEqualTo(HomaFixtures.PROTECTION_WINDOW.end());
        assertThat(payout.completedAt()).isEmpty();
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.released()).isEmpty();
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("The job is marked Completed on Saturday, no report is filed, and the Protection Window ends "
            + "Sunday 14:00. The payout is released then.")
    void payoutIsReleasedWhenTheProtectionWindowEndsAfterTheJobIsCompleted() {
        Payout payout = kemasHomesPayoutAtReserved();

        payout.markCompleted(MARKED_COMPLETED_AT);

        assertThat(payout.completedAt()).contains(MARKED_COMPLETED_AT);
        assertThat(payout.releasableAt(MARKED_COMPLETED_AT)).isFalse();

        PayoutRelease release = payout.release(at(2026, 12, 6, 14, 0));

        assertThat(release.payoutId()).isEqualTo(KEMAS_HOMES_PAYOUT);
        assertThat(release.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(release.releasedAt()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(payout.status()).isEqualTo(PayoutStatus.RELEASED);
        assertThat(payout.released()).contains(release);
    }

    @Test
    @Tag("PAY-2")
    @Tag("JOB-5")
    @DisplayName("Kemas Home hasn't marked the job Completed by Sunday 14:00: the payout is held until it does.")
    void jobPayoutWaitsUntilTheJobIsMarkedCompleted() {
        Payout payout = kemasHomesPayoutAtReserved();

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();
        assertThatThrownBy(() -> payout.release(PROTECTION_WINDOW_END))
                .isInstanceOf(PayoutNotReleasableException.class)
                .hasMessageContaining(KEMAS_HOMES_PAYOUT.value());
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("At Sunday 13:59, one minute before the Protection Window ends, the payout waits.")
    void payoutIsNotReleasableBeforeTheProtectionWindowEnds() {
        Payout payout = kemasHomesJobPayout();

        assertThat(payout.releasableAt(ONE_MINUTE_BEFORE_WINDOW_ENDS)).isFalse();
        assertThatThrownBy(() -> payout.release(ONE_MINUTE_BEFORE_WINDOW_ENDS))
                .isInstanceOf(PayoutNotReleasableException.class)
                .hasMessageContaining(KEMAS_HOMES_PAYOUT.value());
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.released()).isEmpty();
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("At Sunday 14:00 exactly, when the Protection Window ends, the payout is releasable.")
    void payoutIsReleasableExactlyWhenTheProtectionWindowEnds() {
        assertThat(kemasHomesJobPayout().releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-2")
    @Tag("RPT-6")
    @DisplayName("Aisyah's report is open on Sunday 14:00, when the Protection Window ends. Kemas Home's payout "
            + "waits until Support resolves it.")
    void payoutWaitsWhileAReportIsOpen() {
        Payout payout = kemasHomesJobPayout();
        payout.reportOpened(VASE_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();

        payout.reportResolved(VASE_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("RPT-6")
    @DisplayName("Aisyah reports at 11:00 on Saturday, before Kemas Home marks the job Completed at 14:00: "
            + "the payout still waits for that report.")
    void reportMadeBeforeTheJobIsMarkedCompletedHoldsThePayout() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.reportOpened(BATHROOM_REPORT);

        payout.markCompleted(MARKED_COMPLETED_AT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();

        payout.reportResolved(BATHROOM_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-2")
    @Tag("RPT-6")
    @DisplayName("Aisyah reports a cracked vase and an uncleaned bathroom: the payout waits until both are resolved.")
    void payoutWaitsUntilEveryReportIsResolved() {
        Payout payout = kemasHomesJobPayout();
        payout.reportOpened(VASE_REPORT);
        payout.reportOpened(BATHROOM_REPORT);
        payout.reportOpened(VASE_REPORT);

        payout.reportResolved(VASE_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();

        payout.reportResolved(BATHROOM_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("A report this payout never saw open is resolved: the payout is still released on Sunday 14:00.")
    void resolvingAReportNeverOpenedChangesNothing() {
        Payout payout = kemasHomesJobPayout();

        payout.reportResolved(VASE_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("Kemas Home forgets and marks the job Completed on Monday. The payout is released after that, "
            + "with no fine.")
    void jobMarkedCompletedAfterTheProtectionWindowIsReleasableAtOnce() {
        Instant markedCompletedOnMonday = at(2026, 12, 7, 9, 0);
        Payout payout = kemasHomesPayoutAtReserved();

        assertThat(payout.releasableAt(markedCompletedOnMonday)).isFalse();

        payout.markCompleted(markedCompletedOnMonday);

        assertThat(payout.releasableAt(markedCompletedOnMonday)).isTrue();
        assertThat(payout.release(markedCompletedOnMonday).afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("The job is marked Completed once: a second completion is refused and keeps Saturday 14:00.")
    void jobIsMarkedCompletedOnlyOnce() {
        Payout payout = kemasHomesJobPayout();

        assertThatThrownBy(() -> payout.markCompleted(at(2026, 12, 5, 15, 0)))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class)
                .hasMessageContaining(KEMAS_HOMES_PAYOUT.value());
        assertThat(payout.completedAt()).contains(MARKED_COMPLETED_AT);
    }

    @Test
    @Tag("PAY-6")
    @Tag("PAY-7")
    @DisplayName("Aisyah cancels on Friday 22:00 and pays RM45. Kemas Home's RM45 fee waits until Sunday 14:00, "
            + "provided no report is open; no job needs to be marked Completed.")
    void feePayoutWaitsForTheProtectionWindowOfTheBookedJob() {
        Payout payout = kemasHomesCancellationFeePayout();

        assertThat(payout.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(payout.gross()).isEqualTo(Money.myr(45));
        assertThat(payout.protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(payout.releasableAt(CANCELLED_AT)).isFalse();
        assertThat(payout.releasableAt(ONE_MINUTE_BEFORE_WINDOW_ENDS)).isFalse();
        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-6")
    @Tag("RPT-6")
    @DisplayName("Aisyah reported on Wednesday and cancels on Friday 22:00: the RM45 fee waits for that report too.")
    void feePayoutKeepsTheReportsOpenedBeforeTheCancellation() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.reportOpened(VASE_REPORT);

        payout.payFeeInstead(Money.myr(45));

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();

        payout.reportResolved(VASE_REPORT);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-6")
    @DisplayName("A fee payout has no job to mark Completed, and a completed job's payout can't become a fee.")
    void onlyAJobPayoutWaitingForCompletionBecomesAFee() {
        Payout fee = kemasHomesCancellationFeePayout();
        Payout completed = kemasHomesJobPayout();

        assertThatThrownBy(() -> fee.markCompleted(MARKED_COMPLETED_AT))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class);
        assertThatThrownBy(() -> fee.payFeeInstead(Money.myr(65)))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class);
        assertThatThrownBy(() -> completed.payFeeInstead(Money.myr(45)))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class);
        assertThat(fee.gross()).isEqualTo(Money.myr(45));
        assertThat(completed.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(completed.gross()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A fee of RM0 or RM45.005 is refused: Settlement lines arrive rounded to the sen.")
    void feeIsAPositiveAmountInWholeSen() {
        Payout payout = kemasHomesPayoutAtReserved();

        assertThatThrownBy(() -> payout.payFeeInstead(Money.myr(0)))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("fee payout");
        assertThatThrownBy(() -> payout.payFeeInstead(Money.myr("45.005")))
                .isInstanceOf(AmountNotPayableException.class);
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
    }

    @Test
    @Tag("RPT-9")
    @Tag("PAY-7")
    @DisplayName("Nurul leaves at 12:00 and Aisyah is refunded RM45 at once: Kemas Home's payout drops to RM155.")
    void partialRefundReducesThePayout() {
        Payout payout = kemasHomesPayoutAtReserved();

        payout.reduceByRefund(Money.myr(45));

        assertThat(payout.gross()).isEqualTo(Money.myr(155));
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Aisyah accepts Siti alone on Friday and gets RM90 back: Kemas Home's payout drops to RM110.")
    void smallerRosterRefundReducesThePayout() {
        Payout payout = kemasHomesPayoutAtReserved();

        payout.reduceByRefund(Money.myr(90));

        assertThat(payout.gross()).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("RPT-9")
    @DisplayName("Refunds of RM155 and then RM67.50 leave Kemas Home's RM200 payout at RM0, never below.")
    void payoutNeverDropsBelowZero() {
        Payout payout = jobPayoutAfterRefunds();

        assertThat(payout.gross()).isEqualTo(Money.myr(0));
        PayoutRelease release = payout.release(PROTECTION_WINDOW_END);
        assertThat(release.platformFee()).isEqualTo(Money.myr(0));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(0));
    }

    private static Payout jobPayoutAfterRefunds() {
        Payout payout = kemasHomesJobPayout();
        payout.reduceByRefund(Money.myr(155));
        payout.reduceByRefund(Money.myr("67.50"));
        return payout;
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A refund of RM0 or RM39.375 can't reduce the payout: Settlement lines arrive rounded to the sen.")
    void payoutIsReducedOnlyByARefundInWholeSen() {
        Payout payout = kemasHomesPayoutAtReserved();

        assertThatThrownBy(() -> payout.reduceByRefund(Money.myr(0)))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("refund");
        assertThatThrownBy(() -> payout.reduceByRefund(Money.myr("39.375")))
                .isInstanceOf(AmountNotPayableException.class);
        assertThat(payout.gross()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("Kemas Home's payout was released on Sunday 14:00: it is not released a second time, and a later "
            + "refund can't reduce it.")
    void payoutIsReleasedOnlyOnce() {
        Payout payout = kemasHomesJobPayout();
        payout.release(PROTECTION_WINDOW_END);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END.plusSeconds(60))).isFalse();
        assertThatThrownBy(() -> payout.release(PROTECTION_WINDOW_END.plusSeconds(60)))
                .isInstanceOf(PayoutNotReleasableException.class);
        assertThatThrownBy(() -> payout.reduceByRefund(Money.myr(45)))
                .isInstanceOf(PayoutNotPendingException.class)
                .hasMessageContaining("RELEASED");
        assertThat(payout.gross()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("RPT-6")
    @DisplayName("Kemas Home's payout was released on Sunday 14:00: a report opened after that can't hold it.")
    void releasedPayoutCannotBeHeldByAReport() {
        Payout payout = kemasHomesJobPayout();
        payout.release(PROTECTION_WINDOW_END);

        assertThatThrownBy(() -> payout.reportOpened(VASE_REPORT))
                .isInstanceOf(PayoutNotPendingException.class)
                .hasMessageContaining("RELEASED");
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Kemas Home marked the job Completed but a Provider No Show is confirmed: its payout is voided "
            + "and never released.")
    void voidedPayoutIsNeverReleased() {
        Payout payout = kemasHomesJobPayout();

        payout.makeVoid();

        assertThat(payout.status()).isEqualTo(PayoutStatus.VOID);
        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isFalse();
        assertThatThrownBy(() -> payout.reportOpened(VASE_REPORT))
                .isInstanceOf(PayoutNotPendingException.class)
                .hasMessageContaining("VOID");
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("A voided payout can't be marked completed or turned into a fee.")
    void voidedPayoutCannotChange() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.makeVoid();

        assertThatThrownBy(() -> payout.markCompleted(MARKED_COMPLETED_AT))
                .isInstanceOf(PayoutNotPendingException.class);
        assertThatThrownBy(() -> payout.payFeeInstead(Money.myr(45)))
                .isInstanceOf(PayoutNotPendingException.class);
        assertThat(payout.completedAt()).isEmpty();
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Kemas Home's payout was already released on Sunday 14:00: it can no longer be voided.")
    void releasedPayoutCannotBeVoided() {
        Payout payout = kemasHomesJobPayout();
        payout.release(PROTECTION_WINDOW_END);

        assertThatThrownBy(payout::makeVoid).isInstanceOf(PayoutNotPendingException.class);
        assertThat(payout.status()).isEqualTo(PayoutStatus.RELEASED);
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A payout of RM0 or RM160.625 is refused: Settlement lines arrive rounded to the sen.")
    void payoutIsAPositiveAmountInWholeSen() {
        assertThatThrownBy(() -> PaymentFixtures.payoutAtReservedOf(Money.myr(0), REQUESTED_AT))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("payout");
        assertThatThrownBy(() -> PaymentFixtures.payoutAtReservedOf(Money.myr("160.625"), REQUESTED_AT))
                .isInstanceOf(AmountNotPayableException.class);
    }
}
