package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.domain.PaymentFixtures.CANCELLED_AT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOME;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.payment.domain.exception.AmountNotPayableException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderFineAccountTest {

    private static final Instant NO_SHOW_FINED_AT = at(2026, 12, 5, 10, 30);
    private static final ReservationId EARLIER_RESERVATION = new ReservationId("reservation-earlier");
    private static final Money NOTHING = Money.myr(0);
    /** A RM300 payout after the 15.5% platform fee (PAY-5). */
    private static final Money RM300_AFTER_FEE = Money.myr("253.50");

    /** A made-up later change: from Tuesday 1 December 2026 a 25% fine deduction cap. */
    private static final Instant CAP_RAISED_FROM = at(2026, 12, 1, 0, 0);
    private static final PaymentValues.Version CAP_RAISED =
            new PaymentValues.Version(CAP_RAISED_FROM, Percentage.of("15.5"), Percentage.of(25));

    private static ProviderFineAccount kemasHomeOwingTheNoShowFine() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(STANDARD_RESERVATION, Money.myr(105), NO_SHOW_FINED_AT);
        return account;
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("After a provider no-show, Kemas Home's RM105 fine is on its account, with the reservation it came "
            + "from.")
    void fineIsRecordedWithTheReservationItCameFrom() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        assertThat(account.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(account.fines()).containsExactly(
                new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr(105), NO_SHOW_FINED_AT));
        assertThat(account.outstanding()).isEqualTo(Money.myr(105));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A provider never fined owes nothing.")
    void newAccountOwesNothing() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);

        assertThat(account.outstanding()).isEqualTo(NOTHING);
        assertThat(account.fines()).isEmpty();
        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM105. From a RM300 payout (RM253.50 after the fee), RM50.70 is deducted; RM54.30 "
            + "carries over.")
    void deductionIsAtMostTwentyPercentOfThePayoutAfterThePlatformFee() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        Money deducted = account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT);

        assertThat(deducted).isEqualTo(Money.myr("50.70"));
        assertThat(account.outstanding()).isEqualTo(Money.myr("54.30"));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home's RM105 fine is recovered from future payouts, at most 20% each time, until cleared.")
    void fineCarriesOverFromPayoutToPayoutUntilCleared() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(Money.myr("50.70"));
        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(Money.myr("50.70"));
        assertThat(account.outstanding()).isEqualTo(Money.myr("3.60"));
        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(Money.myr("3.60"));
        assertThat(account.outstanding()).isEqualTo(NOTHING);
        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM10. A RM300 payout pays only that: a deduction is never more than is owed.")
    void deductionIsNeverMoreThanIsOwed() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(STANDARD_RESERVATION, Money.myr(10), NO_SHOW_FINED_AT);

        assertThat(account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(Money.myr(10));
        assertThat(account.outstanding()).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM45 from an earlier reservation and RM105 from the no-show. Its next payout clears "
            + "the older fine first.")
    void oldestFineIsClearedFirst() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(EARLIER_RESERVATION, Money.myr(45), CANCELLED_AT);
        account.recordFine(STANDARD_RESERVATION, Money.myr(105), NO_SHOW_FINED_AT);

        Money deducted = account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT);

        assertThat(deducted).isEqualTo(Money.myr("50.70"));
        assertThat(account.fines()).containsExactly(
                new Fine(EARLIER_RESERVATION, Money.myr(45), NOTHING, CANCELLED_AT),
                new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr("99.30"), NO_SHOW_FINED_AT));
        assertThat(account.outstanding()).isEqualTo(Money.myr("99.30"));

        account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT);

        assertThat(account.fines()).containsExactly(
                new Fine(EARLIER_RESERVATION, Money.myr(45), NOTHING, CANCELLED_AT),
                new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr("48.60"), NO_SHOW_FINED_AT));
    }

    @Test
    @Tag("PAY-5")
    @Tag("PAY-6")
    @DisplayName("A RM45 cancellation fee pays Kemas Home RM38.02 after the platform fee: a fine is deducted from a "
            + "fee payout too, 20% of RM38.02 being RM7.60.")
    void feePayoutsAreDeductedFromToo() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        Money deducted = account.deductFrom(Money.myr("38.02"), REQUESTED_AT);

        assertThat(deducted).isEqualTo(Money.myr("7.60"));
        assertThat(account.outstanding()).isEqualTo(Money.myr("97.40"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("The deduction is rounded once to the sen, half up: 20% of RM38.02 is RM7.604, so RM7.60; of RM38.03 "
            + "it is RM7.606, so RM7.61.")
    void deductionIsRoundedOnceToTheSen() {
        ProviderFineAccount roundedDown = kemasHomeOwingTheNoShowFine();
        ProviderFineAccount roundedUp = kemasHomeOwingTheNoShowFine();

        assertThat(roundedDown.deductFrom(Money.myr("38.02"), REQUESTED_AT)).isEqualTo(Money.myr("7.60"));
        assertThat(roundedUp.deductFrom(Money.myr("38.03"), REQUESTED_AT)).isEqualTo(Money.myr("7.61"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A rounded deduction never takes more than the provider owes: RM7.60 owed, RM7.61 allowed, RM7.60 "
            + "taken.")
    void roundedDeductionIsStillCappedByWhatIsOwed() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(STANDARD_RESERVATION, Money.myr("7.60"), NO_SHOW_FINED_AT);

        assertThat(account.deductFrom(Money.myr("38.03"), REQUESTED_AT)).isEqualTo(Money.myr("7.60"));
        assertThat(account.outstanding()).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Everything was refunded to Aisyah, so the payout is RM0: nothing is deducted.")
    void nothingIsDeductedFromAnEmptyPayout() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        assertThat(account.deductFrom(NOTHING, REQUESTED_AT)).isEqualTo(NOTHING);
        assertThat(account.outstanding()).isEqualTo(Money.myr(105));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-5")
    @DisplayName("Homa raises the deduction cap to 25% from Tuesday. A reservation Requested on Monday 20:00 keeps "
            + "20%: RM50.70; one Requested on Tuesday takes 25%: RM63.38.")
    void reservationKeepsTheDeductionCapInForceWhenItWasRequested() {
        ProviderFineAccount keptCap = kemasHomeOwingTheNoShowFine();
        ProviderFineAccount raisedCap = kemasHomeOwingTheNoShowFine();
        PaymentValues values = new PaymentValues(List.of(PaymentValues.current(), CAP_RAISED));

        assertThat(keptCap.deductFrom(RM300_AFTER_FEE, values.inForceAt(REQUESTED_AT)))
                .isEqualTo(Money.myr("50.70"));
        assertThat(raisedCap.deductFrom(RM300_AFTER_FEE, values.inForceAt(CAP_RAISED_FROM)))
                .isEqualTo(Money.myr("63.38"));
        assertThat(REQUESTED_AT).isBefore(CAP_RAISED_FROM);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("A payout after the platform fee is never negative.")
    void payoutAfterThePlatformFeeIsNotNegative() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        assertThatThrownBy(() -> account.deductFrom(Money.myr(-1), REQUESTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
        assertThatThrownBy(() -> account.deductFrom(null, REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThat(account.outstanding()).isEqualTo(Money.myr(105));
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-8")
    @DisplayName("A fine is a positive amount in whole sen, for a known reservation and time.")
    void fineNeedsAPayableAmountItsReservationAndATime() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);

        assertThatThrownBy(() -> account.recordFine(STANDARD_RESERVATION, NOTHING, CANCELLED_AT))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("fine");
        assertThatThrownBy(() -> account.recordFine(STANDARD_RESERVATION, Money.myr("1.005"), CANCELLED_AT))
                .isInstanceOf(AmountNotPayableException.class);
        assertThatThrownBy(() -> account.recordFine(null, Money.myr(1), CANCELLED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> account.recordFine(STANDARD_RESERVATION, Money.myr(1), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ProviderFineAccount.openFor(null)).isInstanceOf(NullPointerException.class);
        assertThat(account.fines()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("The fines listed cannot be changed from outside.")
    void listedFinesCannotBeChanged() {
        ProviderFineAccount account = kemasHomeOwingTheNoShowFine();

        assertThatThrownBy(() -> account.fines().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(account.fines()).hasSize(1);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Persistence: Kemas Home's account comes back with every fine, cleared ones too, and carries on "
            + "deducting from the oldest.")
    void storedAccountKeepsItsFines() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(EARLIER_RESERVATION, Money.myr(45), CANCELLED_AT);
        account.recordFine(STANDARD_RESERVATION, Money.myr(105), NO_SHOW_FINED_AT);
        account.deductFrom(RM300_AFTER_FEE, REQUESTED_AT);

        ProviderFineAccount stored = ProviderFineAccount.restore(KEMAS_HOME, account.fines());

        assertThat(stored).usingRecursiveComparison().isEqualTo(account);
        assertThat(stored.deductFrom(RM300_AFTER_FEE, REQUESTED_AT)).isEqualTo(Money.myr("50.70"));
        assertThat(stored.outstanding()).isEqualTo(Money.myr("48.60"));
        assertThatThrownBy(() -> ProviderFineAccount.restore(null, List.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Persistence: a stored fine owes from nothing up to its amount, never more or less.")
    void storedFineOwesNoMoreThanItsAmount() {
        assertThat(new Fine(STANDARD_RESERVATION, Money.myr(105), NOTHING, CANCELLED_AT).outstanding())
                .isEqualTo(NOTHING);
        assertThat(new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr(105), CANCELLED_AT).outstanding())
                .isEqualTo(Money.myr(105));
        assertThatThrownBy(() -> new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr("105.01"), CANCELLED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outstanding");
        assertThatThrownBy(() -> new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr(-1), CANCELLED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Fine(STANDARD_RESERVATION, Money.myr(105), null, CANCELLED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Fine(STANDARD_RESERVATION, Money.myr(105), Money.myr(1), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Fine(null, Money.myr(105), Money.myr(1), CANCELLED_AT))
                .isInstanceOf(NullPointerException.class);
    }
}
