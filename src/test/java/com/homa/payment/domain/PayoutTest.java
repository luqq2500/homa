package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.domain.PaymentFixtures.BATHROOM_REPORT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOME;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_PAYOUT;
import static com.homa.payment.domain.PaymentFixtures.MARKED_COMPLETED_AT;
import static com.homa.payment.domain.PaymentFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_RESERVATION;
import static com.homa.payment.domain.PaymentFixtures.VASE_REPORT;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesCancellationFeePayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesJobPayout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
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
    @Tag("PAY-2")
    @DisplayName("The job is marked Completed on Saturday, no report is filed, and the Protection Window ends "
            + "Sunday 14:00. The payout is released then.")
    void payoutIsReleasedWhenTheProtectionWindowEndsAfterTheJobIsCompleted() {
        Payout payout = kemasHomesJobPayout();

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
        Payout payout = kemasHomesJobPayout();

        assertThat(payout.releasableAt(markedCompletedOnMonday)).isTrue();
        assertThat(payout.release(markedCompletedOnMonday).platformFee()).isEqualTo(Money.myr(31));
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("The job is marked Completed: Kemas Home's RM200 payout opens for the reservation, due Sunday 14:00.")
    void completedJobOpensAJobPayoutDueWhenTheProtectionWindowEnds() {
        Payout payout = kemasHomesJobPayout();

        assertThat(payout.id()).isEqualTo(KEMAS_HOMES_PAYOUT);
        assertThat(payout.reservationId()).isEqualTo(STANDARD_RESERVATION);
        assertThat(payout.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(payout.gross()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(payout.protectionWindowEnd()).isEqualTo(HomaFixtures.PROTECTION_WINDOW.end());
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
    }

    @Test
    @Tag("PAY-6")
    @DisplayName("Aisyah cancels on Friday 22:00 and pays RM45. Kemas Home receives the RM45 on Sunday at 14:00, "
            + "provided no report is open.")
    void feePayoutWaitsForTheProtectionWindowOfTheBookedJob() {
        Payout payout = kemasHomesCancellationFeePayout();

        assertThat(payout.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(payout.gross()).isEqualTo(Money.myr(45));
        assertThat(payout.releasableAt(ONE_MINUTE_BEFORE_WINDOW_ENDS)).isFalse();
        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("Kemas Home's payout was released on Sunday 14:00: it is not released a second time.")
    void payoutIsReleasedOnlyOnce() {
        Payout payout = kemasHomesJobPayout();
        payout.release(PROTECTION_WINDOW_END);

        assertThat(payout.releasableAt(PROTECTION_WINDOW_END.plusSeconds(60))).isFalse();
        assertThatThrownBy(() -> payout.release(PROTECTION_WINDOW_END.plusSeconds(60)))
                .isInstanceOf(PayoutNotReleasableException.class);
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
        assertThatThrownBy(() -> PaymentFixtures.jobPayoutOf(Money.myr(0)))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("payout");
        assertThatThrownBy(() -> PaymentFixtures.jobPayoutOf(Money.myr("160.625")))
                .isInstanceOf(AmountNotPayableException.class);
    }
}
