package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOME;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_PAYOUT;
import static com.homa.payment.domain.PaymentFixtures.MARKED_COMPLETED_AT;
import static com.homa.payment.domain.PaymentFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.domain.PaymentFixtures.jobPayoutOf;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesCancellationFeePayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesJobPayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesPayoutAtReserved;
import static com.homa.payment.domain.PaymentFixtures.payoutAtReservedOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.payment.domain.exception.PayoutNotReleasableException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PayoutReleaseTest {

    /** A made-up later change: from Tuesday 1 December 2026 the platform fee is 18%. */
    private static final Instant FEE_RAISED_FROM = at(2026, 12, 1, 0, 0);
    private static final PaymentValues FEE_RAISED_ON_TUESDAY = new PaymentValues(List.of(
            PaymentValues.current(),
            new PaymentValues.Version(FEE_RAISED_FROM, Percentage.of(18), Percentage.of(20))));

    @Test
    @Tag("PAY-3")
    @DisplayName("A RM200 payout pays Kemas Home RM169.")
    void platformFeeIsTakenFromAJobPayout() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(release.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(release.gross()).isEqualTo(Money.myr(200));
        assertThat(release.platformFee()).isEqualTo(Money.myr(31));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("PAY-3")
    @Tag("PAY-6")
    @DisplayName("A RM45 cancellation fee pays it RM38.02 after a RM6.98 platform fee.")
    void platformFeeIsTakenFromAFeePayoutToo() {
        PayoutRelease release = kemasHomesCancellationFeePayout().release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(release.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(release.gross()).isEqualTo(Money.myr(45));
        assertThat(release.platformFee()).isEqualTo(Money.myr("6.98"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("38.02"));
    }

    @Test
    @Tag("PAY-8")
    @Tag("RPT-9")
    @DisplayName("Nurul leaves at 12:05 and the reservation total becomes RM160.62 before the platform fee: "
            + "15.5% is RM24.8961, rounded once to RM24.90, so Kemas Home receives RM135.72.")
    void platformFeeIsRoundedOnceToTheSenAndTheLinesAddUp() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.reduceByRefund(Money.myr("39.38"));
        payout.markCompleted(MARKED_COMPLETED_AT);

        PayoutRelease release = payout.release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(release.gross()).isEqualTo(Money.myr("160.62"));
        assertThat(release.platformFee()).isEqualTo(Money.myr("24.90"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("135.72"));
        assertThat(release.afterPlatformFee().plus(release.platformFee())).isEqualTo(release.gross());
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A RM100.10 payout gives a RM15.5155 fee, rounded half up once to RM15.52.")
    void platformFeeRoundsHalfUpToTheSen() {
        PayoutRelease release = jobPayoutOf(Money.myr("100.10")).release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(release.platformFee()).isEqualTo(Money.myr("15.52"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("84.58"));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-3")
    @DisplayName("Homa raises the platform fee from Tuesday. Aisyah's reservation, Requested on Monday 20:00, keeps "
            + "15.5%: Kemas Home still receives RM169.")
    void laterPlatformFeeDoesNotChangeAReservationRequestedEarlier() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing(), FEE_RAISED_ON_TUESDAY);

        assertThat(release.platformFee()).isEqualTo(Money.myr(31));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-3")
    @DisplayName("A reservation Requested on Tuesday, after Homa raised the platform fee, pays the new rate: "
            + "18% of RM200 is RM36.")
    void reservationRequestedAfterAChangePaysTheNewPlatformFee() {
        Payout payout = payoutAtReservedOf(TOTAL_RESERVATION_FEE, FEE_RAISED_FROM);
        payout.markCompleted(MARKED_COMPLETED_AT);

        PayoutRelease release = payout.release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing(), FEE_RAISED_ON_TUESDAY);

        assertThat(release.platformFee()).isEqualTo(Money.myr(36));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(164));
        assertThat(REQUESTED_AT).isBefore(FEE_RAISED_FROM);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM105. From a RM300 payout (RM253.50 after the fee), RM50.70 is deducted and "
            + "RM202.80 is sent; RM54.30 carries over.")
    void fineIsDeductedAfterThePlatformFee() {
        ProviderFineAccount fines = PaymentFixtures.owingTheNoShowFine();

        PayoutRelease release = jobPayoutOf(Money.myr(300)).release(PROTECTION_WINDOW_END, fines);

        assertThat(release.gross()).isEqualTo(Money.myr(300));
        assertThat(release.platformFee()).isEqualTo(Money.myr("46.50"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("253.50"));
        assertThat(release.fineDeduction()).isEqualTo(Money.myr("50.70"));
        assertThat(release.net()).isEqualTo(Money.myr("202.80"));
        assertThat(release.platformFee().plus(release.fineDeduction()).plus(release.net()))
                .isEqualTo(release.gross());
        assertThat(fines.outstanding()).isEqualTo(Money.myr("54.30"));
    }

    @Test
    @Tag("PAY-5")
    @Tag("PAY-6")
    @DisplayName("Aisyah's RM45 fee payout pays Kemas Home RM38.02 after the platform fee; owing RM105, RM7.60 is "
            + "deducted and RM30.42 is sent: a fine comes off a fee payout too.")
    void fineIsDeductedFromAFeePayoutToo() {
        ProviderFineAccount fines = PaymentFixtures.owingTheNoShowFine();

        PayoutRelease release = kemasHomesCancellationFeePayout().release(PROTECTION_WINDOW_END, fines);

        assertThat(release.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(release.platformFee()).isEqualTo(Money.myr("6.98"));
        assertThat(release.fineDeduction()).isEqualTo(Money.myr("7.60"));
        assertThat(release.net()).isEqualTo(Money.myr("30.42"));
        assertThat(fines.outstanding()).isEqualTo(Money.myr("97.40"));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes nothing: the whole RM169 after the platform fee is sent.")
    void nothingIsDeductedFromAProviderThatOwesNothing() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(release.fineDeduction()).isEqualTo(Money.myr(0));
        assertThat(release.net()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-5")
    @DisplayName("Homa raises the deduction cap to 25% from Tuesday. A reservation Requested on Monday 20:00 keeps 20%: "
            + "RM50.70 of RM253.50; one Requested on Tuesday takes 25%: RM63.38.")
    void reservationKeepsTheDeductionCapInForceWhenItWasRequested() {
        PaymentValues capRaised = new PaymentValues(List.of(PaymentValues.current(),
                new PaymentValues.Version(FEE_RAISED_FROM, Percentage.of("15.5"), Percentage.of(25))));
        Payout requestedOnMonday = jobPayoutOf(Money.myr(300));
        Payout requestedOnTuesday = payoutAtReservedOf(Money.myr(300), FEE_RAISED_FROM);
        requestedOnTuesday.markCompleted(MARKED_COMPLETED_AT);

        PayoutRelease kept = requestedOnMonday.release(PROTECTION_WINDOW_END, PaymentFixtures.owingTheNoShowFine(),
                capRaised);
        PayoutRelease raised = requestedOnTuesday.release(PROTECTION_WINDOW_END,
                PaymentFixtures.owingTheNoShowFine(), capRaised);

        assertThat(kept.fineDeduction()).isEqualTo(Money.myr("50.70"));
        assertThat(raised.fineDeduction()).isEqualTo(Money.myr("63.38"));
        assertThat(raised.net()).isEqualTo(Money.myr("190.12"));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("A payout is released against its own provider's fines, never another provider's.")
    void payoutIsReleasedAgainstItsOwnProvidersFines() {
        Payout payout = kemasHomesJobPayout();
        ProviderFineAccount hanaCleaning = ProviderFineAccount.openFor(new ProviderId("provider-hana-cleaning"));

        assertThatThrownBy(() -> payout.release(PROTECTION_WINDOW_END, hanaCleaning))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider-kemas-home")
                .hasMessageContaining("provider-hana-cleaning");
        assertThatThrownBy(() -> payout.release(PROTECTION_WINDOW_END, null))
                .isInstanceOf(NullPointerException.class);
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("A payout that is not due is not released, and the provider's fines stay as they are.")
    void finesAreUntouchedWhenThePayoutIsNotReleasable() {
        ProviderFineAccount fines = PaymentFixtures.owingTheNoShowFine();

        assertThatThrownBy(() -> kemasHomesPayoutAtReserved().release(PROTECTION_WINDOW_END, fines))
                .isInstanceOf(PayoutNotReleasableException.class);
        assertThat(fines.outstanding()).isEqualTo(Money.myr(105));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("A released payout's lines add up: nothing deducted can be below nothing or above the payout after "
            + "the platform fee.")
    void fineDeductionStaysWithinThePayoutAfterThePlatformFee() {
        PayoutId id = KEMAS_HOMES_PAYOUT;
        Instant at = PROTECTION_WINDOW_END;
        Money gross = Money.myr(45);
        Money fee = Money.myr("6.98");

        assertThat(new PayoutRelease(id, KEMAS_HOME, PayoutKind.FEE, gross, fee, Money.myr("38.02"), at).net())
                .isEqualTo(Money.myr(0));
        assertThat(new PayoutRelease(id, KEMAS_HOME, PayoutKind.FEE, gross, fee, Money.myr(0), at).net())
                .isEqualTo(Money.myr("38.02"));
        assertThatThrownBy(() -> new PayoutRelease(id, KEMAS_HOME, PayoutKind.FEE, gross, fee, Money.myr("38.03"), at))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fine deduction");
        assertThatThrownBy(() -> new PayoutRelease(id, KEMAS_HOME, PayoutKind.FEE, gross, fee, Money.myr(-1), at))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PayoutRelease(id, KEMAS_HOME, PayoutKind.FEE, gross, fee, null, at))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Releasing RM300 from a provider owing RM105 posts four ledger lines: the held money becomes owed to "
            + "Kemas Home, Homa takes RM46.50, RM50.70 clears the fine, RM202.80 is sent.")
    void releasePostsEveryLineToTheLedger() {
        PayoutRelease release = jobPayoutOf(Money.myr(300)).release(PROTECTION_WINDOW_END,
                PaymentFixtures.owingTheNoShowFine());

        assertThat(release.ledgerEntries()).containsExactly(
                new LedgerEntry(LedgerMovement.PAYOUT_RELEASED, Money.myr(300), PROTECTION_WINDOW_END),
                new LedgerEntry(LedgerMovement.PLATFORM_FEE_TAKEN, Money.myr("46.50"), PROTECTION_WINDOW_END),
                new LedgerEntry(LedgerMovement.FINE_DEDUCTED, Money.myr("50.70"), PROTECTION_WINDOW_END),
                new LedgerEntry(LedgerMovement.PAYOUT_SENT, Money.myr("202.80"), PROTECTION_WINDOW_END));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A release with no fine to deduct posts no deduction line; one with nothing left posts nothing.")
    void releasePostsOnlyTheLinesThatMoveMoney() {
        PayoutRelease untouched = kemasHomesJobPayout().release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());
        Payout refundedInFull = kemasHomesJobPayout();
        refundedInFull.reduceByRefund(Money.myr(200));
        PayoutRelease nothingLeft = refundedInFull.release(PROTECTION_WINDOW_END, PaymentFixtures.owingNothing());

        assertThat(untouched.ledgerEntries()).extracting(LedgerEntry::movement).containsExactly(
                LedgerMovement.PAYOUT_RELEASED, LedgerMovement.PLATFORM_FEE_TAKEN, LedgerMovement.PAYOUT_SENT);
        assertThat(nothingLeft.ledgerEntries()).isEmpty();
    }
}
