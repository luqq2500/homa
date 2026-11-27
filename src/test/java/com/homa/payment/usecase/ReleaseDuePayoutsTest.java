package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_CHECKED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_NUMBER;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.BANK;
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
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.ReportId;
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
    private final RecordingPayoutGateway gateway = new RecordingPayoutGateway();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(PROTECTION_WINDOW_END);
    private final ReleaseDuePayouts releaser =
            new ReleaseDuePayouts(payouts, accounts, gateway, publisher, time, new SequentialEventIds());

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
                RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(200), Money.myr(31), Money.myr(169),
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
                RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(0), Money.myr(0), Money.myr(0),
                PROTECTION_WINDOW_END));
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
