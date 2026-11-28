package com.homa.payment.domain;

import static com.homa.payment.domain.PaymentFixtures.CANCELLED_AT;
import static com.homa.payment.domain.PaymentFixtures.PAID_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class LedgerTest {

    private static final Money NOTHING = Money.myr(0);

    private static LedgerEntry entry(LedgerMovement movement, String amount) {
        return new LedgerEntry(movement, Money.myr(amount), CANCELLED_AT);
    }

    /** Everything the debit-side accounts hold, which the credit-side accounts must hold too. */
    private static Money debitSide(Ledger ledger) {
        return ledger.balanceOf(LedgerAccount.HOMA_BANK)
                .plus(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE))
                .plus(ledger.balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE));
    }

    private static Money creditSide(Ledger ledger) {
        return ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)
                .plus(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE))
                .plus(ledger.balanceOf(LedgerAccount.HOMA_REVENUE));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Aisyah's RM200 is held, then a provider no-show: she gets RM200 back plus RM20 from Homa, and Kemas "
            + "Home's RM105 fine repays the RM20, so Homa keeps RM85.")
    void homaPaysTheCompensationAndKeepsTheRestOfTheFine() {
        Ledger ledger = new Ledger();

        ledger.post(entry(LedgerMovement.PAYMENT_HELD, "200"));
        ledger.post(entry(LedgerMovement.REFUND_SENT, "200"));
        ledger.post(entry(LedgerMovement.COMPENSATION_PAID, "20"));
        ledger.post(entry(LedgerMovement.FINE_IMPOSED, "105"));

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE)).isEqualTo(Money.myr(20));
        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(Money.myr(105));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr(105));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr(-20));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(85));
    }

    @Test
    @Tag("PAY-6")
    @Tag("PAY-3")
    @DisplayName("Aisyah cancels on Friday 22:00: RM155 goes back at once, and on Sunday 14:00 the RM45 fee pays "
            + "Kemas Home RM38.02 after a RM6.98 platform fee.")
    void cancellationFeeIsHeldThenReleasedLessThePlatformFee() {
        Ledger ledger = new Ledger();
        ledger.post(entry(LedgerMovement.PAYMENT_HELD, "200"));
        ledger.post(entry(LedgerMovement.REFUND_SENT, "155"));

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(45));

        ledger.post(entry(LedgerMovement.PAYOUT_RELEASED, "45"));
        ledger.post(entry(LedgerMovement.PLATFORM_FEE_TAKEN, "6.98"));
        ledger.post(entry(LedgerMovement.PAYOUT_SENT, "38.02"));

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr("6.98"));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr("6.98"));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr("6.98"));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM105. From a RM300 payout (RM253.50 after the fee), RM50.70 is deducted: the fine "
            + "owed falls to RM54.30.")
    void deductedFineClearsTheFineReceivable() {
        Ledger ledger = new Ledger();
        ledger.post(entry(LedgerMovement.FINE_IMPOSED, "105"));
        ledger.post(entry(LedgerMovement.PAYMENT_HELD, "300"));

        ledger.post(entry(LedgerMovement.PAYOUT_RELEASED, "300"));
        ledger.post(entry(LedgerMovement.PLATFORM_FEE_TAKEN, "46.50"));
        ledger.post(entry(LedgerMovement.FINE_DEDUCTED, "50.70"));
        ledger.post(entry(LedgerMovement.PAYOUT_SENT, "202.80"));

        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(Money.myr("54.30"));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr("97.20"));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Every movement debits one account and credits another by the same amount, so the debit side always "
            + "equals the credit side.")
    void everyMovementKeepsTheBooksBalanced() {
        Ledger ledger = new Ledger();
        for (LedgerMovement movement : LedgerMovement.values()) {
            assertThat(movement.debited()).isNotEqualTo(movement.credited());
            ledger.post(entry(movement, "12.34"));

            assertThat(debitSide(ledger)).isEqualTo(creditSide(ledger));
        }
        assertThat(ledger.entries()).hasSize(LedgerMovement.values().length);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("An account nothing touched holds nothing; an entry changes only the two accounts it names.")
    void anEntryTouchesOnlyItsTwoAccounts() {
        Ledger ledger = new Ledger();
        ledger.post(entry(LedgerMovement.COMPENSATION_PAID, "20"));

        assertThat(ledger.balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE)).isEqualTo(Money.myr(20));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr(-20));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(NOTHING);
        assertThat(new Ledger().homaKeeps()).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Homa keeps the fine less the compensation, which can be less than nothing when a fine is smaller "
            + "than the compensation paid.")
    void homaKeepsWhatItEarnsLessWhatItPaidAsCompensation() {
        Ledger ledger = new Ledger();
        ledger.post(entry(LedgerMovement.COMPENSATION_PAID, "20"));
        ledger.post(entry(LedgerMovement.FINE_IMPOSED, "15"));

        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(-5));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("The books are rebuilt from every entry posted, oldest first, and the entries listed cannot be "
            + "changed from outside.")
    void booksAreRestoredFromTheirEntries() {
        List<LedgerEntry> posted = List.of(entry(LedgerMovement.PAYMENT_HELD, "200"),
                new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(45), PAID_AT));

        Ledger restored = Ledger.restore(posted);

        assertThat(restored.entries()).containsExactlyElementsOf(posted);
        assertThat(restored.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(155));
        assertThatThrownBy(() -> restored.entries().add(posted.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A ledger entry is a positive amount in whole sen, with its movement and time.")
    void anEntryNeedsAMovementAPayableAmountAndATime() {
        assertThatThrownBy(() -> new LedgerEntry(LedgerMovement.REFUND_SENT, NOTHING, CANCELLED_AT))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("ledger entry");
        assertThatThrownBy(() -> new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr("1.005"), CANCELLED_AT))
                .isInstanceOf(AmountNotPayableException.class);
        assertThatThrownBy(() -> new LedgerEntry(null, Money.myr(1), CANCELLED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(1), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("The books need an entry to post and an account to read.")
    void postingNothingOrReadingNoAccountIsRefused() {
        Ledger ledger = new Ledger();

        assertThatThrownBy(() -> ledger.post(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ledger.balanceOf(null)).isInstanceOf(NullPointerException.class);
    }
}
