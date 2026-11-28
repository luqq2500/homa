package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_SUBMITTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_NUMBER;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.BANK;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.COMPLETED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.checkoutStarted;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.completed;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesAccountRequest;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.reserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.ie.events.ProviderOpenedIE;
import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.PayoutReleasedIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountResponse;
import com.homa.payment.usecase.ie.HandlePayoutAccountHolderMatchedIE;
import com.homa.payment.usecase.ie.HandleProviderOpenedIE;
import com.homa.payment.usecase.ie.HandleReservationCheckoutStartedIE;
import com.homa.payment.usecase.ie.HandleReservationCompletedIE;
import com.homa.payment.usecase.ie.HandleReservationReservedIE;
import com.homa.payment.usecase.port.out.PayoutGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Flow A (plan, "Key end-to-end flows"), steps 3 and 10, as Payment walks it with every port faked. */
class FlowAPaymentWalkTest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
    private final RecordingPaymentGateway paymentGateway = new RecordingPaymentGateway();
    private final RecordingPayoutGateway payoutGateway = new RecordingPayoutGateway();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(ACCOUNT_SUBMITTED_AT);
    private final SequentialEventIds eventIds = new SequentialEventIds();

    @Test
    @Tag("PRV-2")
    @Tag("PAY-1")
    @Tag("PAY-2")
    @Tag("PAY-3")
    @DisplayName("Kemas Home's account is verified; Aisyah pays RM200; the reservation is Reserved and Completed; "
            + "at Sunday 14:00 the RM31 platform fee is taken and RM169 is sent.")
    void aisyahPaysAndKemasHomeIsPaid() {
        InMemoryProviderOwners owners = new InMemoryProviderOwners();
        new HandleProviderOpenedIE(processed, owners).handle(new ProviderOpenedIE("provider-1",
                KEMAS_HOME_PROVIDER_ID, PaymentUsecaseFixtures.AZMAN, ACCOUNT_SUBMITTED_AT));
        SubmitPayoutAccountResponse account = new ProviderSubmitPayoutAccount(owners, accounts, payoutGateway,
                publisher, time, eventIds).submit(kemasHomesAccountRequest());
        PayoutAccountSubmittedIE submitted = (PayoutAccountSubmittedIE) publisher.published().get(0);
        new HandlePayoutAccountHolderMatchedIE(accounts, publisher, processed, time, eventIds)
                .handle(new PayoutAccountHolderMatchedIE("verification-1", submitted.payoutAccountId(),
                        submitted.providerId(), submitted.holder(), true, ACCOUNT_SUBMITTED_AT));

        new HandleReservationCheckoutStartedIE(holds, processed).handle(checkoutStarted("reservation-1"));
        String reference = new CustomerPayForReservation(holds, paymentGateway)
                .pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)).paymentReference();
        time.set(PAID_AT);
        new ConfirmGatewayPayment(holds, paymentGateway, publisher, processed, time, eventIds)
                .confirm(PaymentUsecaseFixtures.confirmed("gw-1", new PaymentAttemptId(reference), PAID_AT));

        new HandleReservationReservedIE(payouts, processed).handle(reserved("reservation-2"));
        time.set(COMPLETED_AT);
        new HandleReservationCompletedIE(payouts, processed).handle(completed("reservation-3"));
        time.set(PROTECTION_WINDOW_END);
        new ReleaseDuePayouts(payouts, accounts, new InMemoryFineAccountRepository(), payoutGateway,
                new InMemoryLedgerRepository(), publisher, time, eventIds)
                .releaseDue(new ReleaseDuePayoutsRequest());

        String payoutId = payouts.findByReservationId(RESERVATION).orElseThrow().id().value();
        assertThat(publisher.published()).containsExactly(
                new PayoutAccountSubmittedIE("event-1", KEMAS_HOME_PROVIDER_ID, account.payoutAccountId(),
                        "gateway-account-1", "BUSINESS", ACCOUNT_SUBMITTED_AT),
                new PayoutAccountVerifiedIE("event-2", KEMAS_HOME_PROVIDER_ID, account.payoutAccountId(),
                        "BUSINESS", true, ACCOUNT_SUBMITTED_AT),
                new PaymentHeldIE("event-3", CHECKOUT_ID, Money.myr(200), PAID_AT, PAID_AT),
                new PayoutReleasedIE("event-4", payoutId, RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "JOB",
                        Money.myr(200), Money.myr(31), Money.myr(0), Money.myr(169), PROTECTION_WINDOW_END));
        assertThat(paymentGateway.refunds()).isEmpty();
        assertThat(payoutGateway.transfers()).containsExactly(new PayoutGateway.Transfer("payout:" + payoutId,
                account.payoutAccountId(), BANK, ACCOUNT_NUMBER, KEMAS_HOME_NAME, Money.myr(169)));
    }
}
