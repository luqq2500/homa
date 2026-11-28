package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_CHECKED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_NUMBER;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.BANK;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAYOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesCompletedPayout;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesPayoutAtReserved;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesSubmittedAccount;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesVerifiedAccount;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutReleasedIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.ProviderFineAccount;
import com.homa.payment.domain.ReportId;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsResponse;
import com.homa.payment.usecase.port.out.PayoutGateway;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReleaseDuePayoutsTest {

    private static final Instant ONE_MINUTE_BEFORE_WINDOW_ENDS = PROTECTION_WINDOW_END.minusSeconds(60);

    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
    private final InMemoryFineAccountRepository fineAccounts = new InMemoryFineAccountRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPayoutGateway gateway = new RecordingPayoutGateway();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(PROTECTION_WINDOW_END);
    private final ReleaseDuePayouts releaser =
            new ReleaseDuePayouts(payouts, accounts, fineAccounts, gateway, ledger, publisher, time,
                    new SequentialEventIds());

    private ReleaseDuePayoutsResponse releaseDue() {
        return releaser.releaseDue(new ReleaseDuePayoutsRequest());
    }

    @Test
    @Tag("PAY-2")
    @Tag("PAY-3")
    @Tag("PRV-2")
    @DisplayName("Sunday 14:00, no report: Kemas Home's RM200 payout is released, RM31 platform fee, RM169 sent to "
            + "its verified account.")
    void duePayoutIsReleasedAndSentToTheVerifiedAccount() {
        Payout payout = kemasHomesCompletedPayout();
        payouts.given(payout);
        accounts.given(kemasHomesVerifiedAccount());

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.releasedPayoutIds()).containsExactly(PAYOUT_ID);
        assertThat(response.waitingForPayoutAccountIds()).isEmpty();
        assertThat(payout.status()).isEqualTo(PayoutStatus.RELEASED);
        assertThat(payouts.saves()).containsExactly(payout);
        assertThat(gateway.transfers()).containsExactly(new PayoutGateway.Transfer("payout:" + PAYOUT_ID,
                KEMAS_HOMES_ACCOUNT.value(), BANK, ACCOUNT_NUMBER, KEMAS_HOME_NAME, Money.myr(169)));
        assertThat(publisher.published()).containsExactly(new PayoutReleasedIE("event-1", PAYOUT_ID,
                RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(200), Money.myr(31), Money.myr(0), Money.myr(169),
                PROTECTION_WINDOW_END));
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("A released payout is not released again on the next run.")
    void releasedPayoutIsNotReleasedAgain() {
        payouts.given(kemasHomesCompletedPayout());
        accounts.given(kemasHomesVerifiedAccount());
        releaseDue();
        time.set(PROTECTION_WINDOW_END.plusSeconds(3600));

        ReleaseDuePayoutsResponse second = releaseDue();

        assertThat(second.releasedPayoutIds()).isEmpty();
        assertThat(gateway.transfers()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("At Sunday 13:59 the Protection Window hasn't ended: nothing is released.")
    void payoutWaitsUntilTheProtectionWindowEnds() {
        payouts.given(kemasHomesCompletedPayout());
        accounts.given(kemasHomesVerifiedAccount());
        time.set(ONE_MINUTE_BEFORE_WINDOW_ENDS);

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.releasedPayoutIds()).isEmpty();
        assertThat(response.waitingForPayoutAccountIds()).isEmpty();
        assertThat(gateway.transfers()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PAY-2")
    @Tag("RPT-6")
    @DisplayName("The job isn't marked Completed, or a report is open: the payout isn't released, and isn't counted "
            + "as waiting for an account.")
    void payoutTheDomainHoldsIsNotReleased() {
        Payout notCompleted = kemasHomesPayoutAtReserved();
        payouts.given(notCompleted);
        accounts.given(kemasHomesVerifiedAccount());

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.releasedPayoutIds()).isEmpty();
        assertThat(response.waitingForPayoutAccountIds()).isEmpty();
        assertThat(notCompleted.status()).isEqualTo(PayoutStatus.PENDING);

        Payout reported = kemasHomesCompletedPayout();
        reported.reportOpened(new ReportId("report-cracked-vase"));
        payouts.given(reported);

        assertThat(releaseDue().releasedPayoutIds()).isEmpty();
        assertThat(gateway.transfers()).isEmpty();
        assertThat(payouts.saves()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-10")
    @DisplayName("Kemas Home's account is still being checked on Sunday 14:00: the payout waits, and is sent once "
            + "the account is verified.")
    void payoutWaitsForAVerifiedPayoutAccount() {
        Payout payout = kemasHomesCompletedPayout();
        payouts.given(payout);
        PayoutAccount account = kemasHomesSubmittedAccount();
        accounts.given(account);

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.releasedPayoutIds()).isEmpty();
        assertThat(response.waitingForPayoutAccountIds()).containsExactly(PAYOUT_ID);
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payouts.saves()).isEmpty();
        assertThat(gateway.transfers()).isEmpty();
        assertThat(publisher.published()).isEmpty();

        account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT);
        time.set(HomaFixtures.at(2026, 12, 7, 10, 0));

        assertThat(releaseDue().releasedPayoutIds()).containsExactly(PAYOUT_ID);
        assertThat(gateway.transfers()).extracting(PayoutGateway.Transfer::amount).containsExactly(Money.myr(169));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider that never submitted a payout account gets nothing sent; its payout waits.")
    void payoutWaitsWhenTheProviderHasNoPayoutAccount() {
        payouts.given(kemasHomesCompletedPayout());

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.waitingForPayoutAccountIds()).containsExactly(PAYOUT_ID);
        assertThat(gateway.transfers()).isEmpty();
    }

    @Test
    @Tag("PAY-3")
    @DisplayName("Everything was refunded to Aisyah: the RM0 payout is released and published, and nothing is sent.")
    void nothingIsSentWhenNothingIsLeftAfterThePlatformFee() {
        Payout payout = kemasHomesCompletedPayout();
        payout.reduceByRefund(Money.myr(200));
        payouts.given(payout);
        accounts.given(kemasHomesVerifiedAccount());

        ReleaseDuePayoutsResponse response = releaseDue();

        assertThat(response.releasedPayoutIds()).containsExactly(PAYOUT_ID);
        assertThat(gateway.transfers()).isEmpty();
        assertThat(publisher.published()).containsExactly(new PayoutReleasedIE("event-1", PAYOUT_ID,
                RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(0), Money.myr(0), Money.myr(0), Money.myr(0),
                PROTECTION_WINDOW_END));
    }

    /** Kemas Home owes the RM105 fine of a provider no-show (PAY-4). */
    private ProviderFineAccount givenKemasHomeOwesTheNoShowFine() {
        ProviderFineAccount owing = ProviderFineAccount.openFor(KEMAS_HOME);
        owing.recordFine(new ReservationId("reservation-no-show"), Money.myr(105), HomaFixtures.at(2026, 12, 5, 10, 30));
        fineAccounts.given(owing);
        return owing;
    }

    /** A completed RM300 job payout, for another reservation, due at Sunday 14:00 too. */
    private Payout givenAnotherRm300Payout(String suffix) {
        Payout payout = Payout.openForReservation(new PayoutId("payout-" + suffix),
                new ReservationId("reservation-" + suffix), KEMAS_HOME, Money.myr(300), HomaFixtures.REQUESTED_AT,
                PROTECTION_WINDOW_END);
        payout.markCompleted(HomaFixtures.JOB_END);
        payouts.given(payout);
        return payout;
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes RM105. From a RM300 payout (RM253.50 after the fee), RM50.70 is deducted and "
            + "RM202.80 is sent; RM54.30 carries over.")
    void fineIsDeductedFromThePayoutAfterThePlatformFee() {
        ProviderFineAccount owing = givenKemasHomeOwesTheNoShowFine();
        givenAnotherRm300Payout("a");
        accounts.given(kemasHomesVerifiedAccount());

        releaseDue();

        assertThat(gateway.transfers()).extracting(PayoutGateway.Transfer::amount).containsExactly(Money.myr("202.80"));
        assertThat(owing.outstanding()).isEqualTo(Money.myr("54.30"));
        assertThat(fineAccounts.saves()).containsExactly(owing);
        assertThat(publisher.published()).containsExactly(new PayoutReleasedIE("event-1", "payout-a",
                "reservation-a", KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(300), Money.myr("46.50"),
                Money.myr("50.70"), Money.myr("202.80"), PROTECTION_WINDOW_END));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home's fine is recovered from each payout it is paid, at most 20% each time: two RM300 payouts "
            + "release RM50.70 each, and RM3.60 is still owed.")
    void fineCarriesOverFromOneReleaseToTheNext() {
        ProviderFineAccount owing = givenKemasHomeOwesTheNoShowFine();
        givenAnotherRm300Payout("a");
        givenAnotherRm300Payout("b");
        accounts.given(kemasHomesVerifiedAccount());

        releaseDue();

        assertThat(gateway.transfers()).extracting(PayoutGateway.Transfer::amount)
                .containsExactly(Money.myr("202.80"), Money.myr("202.80"));
        assertThat(owing.outstanding()).isEqualTo(Money.myr("3.60"));
        assertThat(publisher.published()).extracting(event -> ((PayoutReleasedIE) event).fineDeduction())
                .containsExactly(Money.myr("50.70"), Money.myr("50.70"));
    }

    @Test
    @Tag("PAY-5")
    @Tag("PAY-6")
    @DisplayName("Aisyah's RM45 cancellation fee pays Kemas Home RM38.02 at Sunday 14:00; owing RM105, RM7.60 comes "
            + "off it and RM30.42 is sent.")
    void fineIsDeductedFromAFeePayoutToo() {
        ProviderFineAccount owing = givenKemasHomeOwesTheNoShowFine();
        Payout payout = kemasHomesPayoutAtReserved();
        payout.settleCancellation(Money.myr(45));
        payouts.given(payout);
        accounts.given(kemasHomesVerifiedAccount());

        releaseDue();

        assertThat(gateway.transfers()).extracting(PayoutGateway.Transfer::amount).containsExactly(Money.myr("30.42"));
        assertThat(owing.outstanding()).isEqualTo(Money.myr("97.40"));
        assertThat(publisher.published()).containsExactly(new PayoutReleasedIE("event-1", PAYOUT_ID,
                RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "FEE", Money.myr(45), Money.myr("6.98"), Money.myr("7.60"),
                Money.myr("30.42"), PROTECTION_WINDOW_END));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home owes nothing: no fine account is written, and the payout is sent whole after the fee.")
    void providerThatOwesNothingHasNoFineAccountWritten() {
        payouts.given(kemasHomesCompletedPayout());
        accounts.given(kemasHomesVerifiedAccount());

        releaseDue();

        assertThat(fineAccounts.saves()).isEmpty();
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME)).isEmpty();
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home's account is still being checked on Sunday 14:00: the payout waits, and its fine is not "
            + "touched until it is released.")
    void fineIsNotDeductedFromAPayoutThatWaits() {
        ProviderFineAccount owing = givenKemasHomeOwesTheNoShowFine();
        givenAnotherRm300Payout("a");
        accounts.given(kemasHomesSubmittedAccount());

        releaseDue();

        assertThat(owing.outstanding()).isEqualTo(Money.myr(105));
        assertThat(fineAccounts.saves()).isEmpty();
        assertThat(ledger.entries()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-6")
    @DisplayName("Aisyah's RM155 was refunded on Friday and the RM45 fee waits until Sunday 14:00: Kemas Home gets "
            + "RM38.02, Homa keeps the RM6.98 platform fee.")
    void releasePostsToTheLedger() {
        payouts.given(kemasHomesCompletedPayout());
        accounts.given(kemasHomesVerifiedAccount());
        ledger.post(new LedgerEntry(LedgerMovement.PAYMENT_HELD, Money.myr(200), PaymentUsecaseFixtures.PAID_AT));

        releaseDue();

        assertThat(ledger.entries()).extracting(LedgerEntry::movement).containsExactly(LedgerMovement.PAYMENT_HELD,
                LedgerMovement.PAYOUT_RELEASED, LedgerMovement.PLATFORM_FEE_TAKEN, LedgerMovement.PAYOUT_SENT);
        assertThat(ledger.entries()).extracting(LedgerEntry::postedAt).containsExactly(PaymentUsecaseFixtures.PAID_AT,
                PROTECTION_WINDOW_END, PROTECTION_WINDOW_END, PROTECTION_WINDOW_END);
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr(31));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr(31));
    }

    @Test
    @Tag("PAY-3")
    @DisplayName("A payout is sent under the same idempotency key whatever the fine takes from it, so a retried "
            + "transfer is paid once.")
    void transferKeyDoesNotDependOnTheFineDeduction() {
        givenKemasHomeOwesTheNoShowFine();
        givenAnotherRm300Payout("a");
        accounts.given(kemasHomesVerifiedAccount());

        releaseDue();

        assertThat(gateway.transfers()).extracting(PayoutGateway.Transfer::idempotencyKey)
                .containsExactly("payout:payout-a");
    }

    @Test
    @DisplayName("A transfer never writes the account number into a log.")
    void transferHidesTheAccountNumber() {
        PayoutGateway.Transfer transfer = new PayoutGateway.Transfer("payout:p-1", "account-1", BANK, ACCOUNT_NUMBER,
                KEMAS_HOME_NAME, Money.myr(169));

        assertThat(transfer.toString()).isEqualTo("Transfer[payout:p-1, account-1, " + Money.myr(169) + "]");
    }

    @Test
    @DisplayName("The run's response cannot be changed afterwards.")
    void responseListsAreCopies() {
        List<String> released = new java.util.ArrayList<>(List.of("p-1"));
        ReleaseDuePayoutsResponse response = new ReleaseDuePayoutsResponse(released, List.of());
        released.add("p-2");

        assertThat(response.releasedPayoutIds()).containsExactly("p-1");
    }
}
