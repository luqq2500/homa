package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutAccountHolderMatchedIE;
import com.homa.ie.events.ReservationCheckoutRejectedIE;
import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.ReservationDeclinedIE;
import com.homa.ie.events.ReservationExpiredIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.BankAccount;
import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.CustomerId;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountHolder;
import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.dto.ConfirmGatewayPaymentRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountRequest;
import java.time.Instant;
import java.util.List;

/** The §0 example as Payment's use cases see it: Kemas Home's payout account, Aisyah's checkout and payout. */
public final class PaymentUsecaseFixtures {

    private PaymentUsecaseFixtures() {
    }

    public static final String CHECKOUT_ID = "checkout-aisyah-standard";
    public static final String RESERVATION_ID = "reservation-aisyah-standard";
    public static final String HOLD_ID = "hold-aisyah-standard";
    public static final String PAYOUT_ID = "payout-kemas-home-standard";
    public static final String MEI_CUSTOMER_ID = "customer-mei";
    /** Azman, Kemas Home's owner (PRV-1), as Verification knows him. */
    public static final String AZMAN = "person-azman";

    public static final CheckoutId CHECKOUT = new CheckoutId(CHECKOUT_ID);
    public static final CustomerId AISYAH = new CustomerId(AISYAH_CUSTOMER_ID);
    public static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);
    public static final ReservationId RESERVATION = new ReservationId(RESERVATION_ID);

    public static final String BANK = "MAYBANK";
    public static final String ACCOUNT_NUMBER = "514011223344";
    public static final BankAccount KEMAS_HOMES_BANK_ACCOUNT = new BankAccount(BANK, ACCOUNT_NUMBER, KEMAS_HOME_NAME);
    public static final PayoutAccountId KEMAS_HOMES_ACCOUNT = new PayoutAccountId("payout-account-kemas-home");

    /** Aisyah's online banking payment, the first one confirmed. */
    public static final PaymentAttemptId ONLINE_BANKING = new PaymentAttemptId("attempt-aisyah-online-banking");
    /** Aisyah's card payment, confirmed after it (RES-12). */
    public static final PaymentAttemptId CARD = new PaymentAttemptId("attempt-aisyah-card");

    /** Azman submits Kemas Home's payout account the week before Aisyah books. */
    public static final Instant ACCOUNT_SUBMITTED_AT = at(2026, 11, 23, 10, 0);
    public static final Instant ACCOUNT_CHECKED_AT = at(2026, 11, 23, 15, 0);
    /** Aisyah starts the checkout at Monday 19:58 and her payment confirms at 20:00. */
    public static final Instant CHECKOUT_STARTED_AT = at(2026, 11, 30, 19, 58);
    public static final Instant PAID_AT = HomaFixtures.REQUESTED_AT;
    public static final Instant SECOND_PAYMENT_AT = at(2026, 11, 30, 20, 2);
    /** REQ-2: Aisyah withdraws on Monday 21:00. */
    public static final Instant WITHDRAWN_AT = at(2026, 11, 30, 21, 0);
    /** REQ-3: Kemas Home declines on Monday 21:30. */
    public static final Instant DECLINED_AT = at(2026, 11, 30, 21, 30);
    /** REQ-4: Kemas Home doesn't respond by Tuesday 20:00. */
    public static final Instant EXPIRED_AT = HomaFixtures.ACCEPTANCE_DEADLINE;
    public static final Instant RESERVED_AT = at(2026, 11, 30, 22, 0);
    public static final Instant COMPLETED_AT = HomaFixtures.JOB_END;
    /** The Protection Window ends Sunday 14:00. */
    public static final Instant PROTECTION_WINDOW_END = HomaFixtures.PROTECTION_WINDOW.end();

    public static SubmitPayoutAccountRequest kemasHomesAccountRequest() {
        return new SubmitPayoutAccountRequest(AZMAN, KEMAS_HOME_PROVIDER_ID, "BUSINESS", BANK, ACCOUNT_NUMBER,
                KEMAS_HOME_NAME);
    }

    public static PayoutAccount kemasHomesSubmittedAccount() {
        return PayoutAccount.submit(KEMAS_HOME, KEMAS_HOMES_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_BANK_ACCOUNT, ACCOUNT_SUBMITTED_AT);
    }

    public static PayoutAccount kemasHomesVerifiedAccount() {
        PayoutAccount account = kemasHomesSubmittedAccount();
        account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT);
        return account;
    }

    public static PayoutAccountHolderMatchedIE holderMatched(String eventId, PayoutAccountId accountId,
            boolean matched) {
        return new PayoutAccountHolderMatchedIE(eventId, accountId.value(), KEMAS_HOME_PROVIDER_ID, "BUSINESS",
                matched, ACCOUNT_CHECKED_AT);
    }

    public static ReservationCheckoutStartedIE checkoutStarted(String eventId) {
        return new ReservationCheckoutStartedIE(eventId, CHECKOUT_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                TOTAL_RESERVATION_FEE, CHECKOUT_STARTED_AT);
    }

    public static ReservationCheckoutRejectedIE checkoutRejected(String eventId) {
        return new ReservationCheckoutRejectedIE(eventId, CHECKOUT_ID, AISYAH_CUSTOMER_ID, "TIME_TAKEN", PAID_AT);
    }

    public static ReservationRequestedIE requested(String eventId) {
        return new ReservationRequestedIE(eventId, RESERVATION_ID, CHECKOUT_ID, AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, HomaFixtures.CHECK_IN_PERIOD.start(), HomaFixtures.JOB_START,
                HomaFixtures.JOB_END, TOTAL_RESERVATION_FEE, HomaFixtures.ACCEPTANCE_DEADLINE,
                PROTECTION_WINDOW_END, HomaFixtures.REQUESTED_AT, HomaFixtures.REQUESTED_AT);
    }

    public static ReservationWithdrawnIE withdrawn(String eventId) {
        return new ReservationWithdrawnIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                WITHDRAWN_AT, HomaFixtures.REQUESTED_AT, WITHDRAWN_AT);
    }

    public static ReservationDeclinedIE declined(String eventId) {
        return new ReservationDeclinedIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                DECLINED_AT, HomaFixtures.REQUESTED_AT, DECLINED_AT);
    }

    public static ReservationExpiredIE expired(String eventId) {
        return new ReservationExpiredIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                EXPIRED_AT, HomaFixtures.REQUESTED_AT, EXPIRED_AT);
    }

    /** CCX-6: Aisyah cancels on Friday 22:00, after Standard's 21:45 cut-off. */
    public static final Instant LATE_CANCELLATION_AT = at(2026, 12, 4, 22, 0);
    /** NSH-3: Support confirms the Provider No Show at 10:30 on Saturday. */
    public static final Instant NO_SHOW_CONFIRMED_AT = at(2026, 12, 5, 10, 30);

    /**
     * A settlement as Reservation publishes it: the cancellation, with the lines Payment executes (§12). The cause
     * is Reservation's; Payment never reads it.
     */
    public static ReservationCancelledIE cancelled(String eventId, String cause, Instant cancelledAt,
            String settlementId, Money customerFee, Money customerRefund, Money compensation, Money providerFine,
            Money providerPayoutGross) {
        return new ReservationCancelledIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, cause,
                cancelledAt, HomaFixtures.REQUESTED_AT, HomaFixtures.CHECK_IN_PERIOD.start(), PROTECTION_WINDOW_END,
                settlementId, customerFee, customerRefund, compensation, providerFine, providerPayoutGross,
                cancelledAt);
    }

    /** CCX-6: Aisyah pays the RM45 Base Cancellation Fee and gets RM155 back. */
    public static ReservationCancelledIE lateCustomerCancellation(String eventId) {
        return cancelled(eventId, "CUSTOMER_CANCELLED", LATE_CANCELLATION_AT, "settlement-late-cancellation",
                Money.myr(45), Money.myr(155), Money.myr(0), Money.myr(0), Money.myr(45));
    }

    /** NSH-3: Kemas Home is fined RM105, and Aisyah gets her RM200 back plus RM20 from Homa. */
    public static ReservationCancelledIE providerNoShow(String eventId) {
        return cancelled(eventId, "PROVIDER_NO_SHOW", NO_SHOW_CONFIRMED_AT, "settlement-no-show", Money.myr(0),
                Money.myr(200), Money.myr(20), Money.myr(105), Money.myr(0));
    }

    public static ReservationReservedIE reserved(String eventId) {
        return new ReservationReservedIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                HomaFixtures.CHECK_IN_PERIOD.start(), HomaFixtures.JOB_START, HomaFixtures.JOB_END,
                TOTAL_RESERVATION_FEE, PROTECTION_WINDOW_END, HomaFixtures.REQUESTED_AT, RESERVED_AT);
    }

    public static ReservationCompletedIE completed(String eventId) {
        return new ReservationCompletedIE(eventId, RESERVATION_ID, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                TOTAL_RESERVATION_FEE, PROTECTION_WINDOW_END, COMPLETED_AT, HomaFixtures.REQUESTED_AT,
                List.of("photo-after"), COMPLETED_AT);
    }

    public static ConfirmGatewayPaymentRequest confirmed(String gatewayEventId, PaymentAttemptId attempt,
            Instant paidAt) {
        return new ConfirmGatewayPaymentRequest(gatewayEventId, attempt.value(), CHECKOUT_ID, true, paidAt, null);
    }

    public static ConfirmGatewayPaymentRequest refused(String gatewayEventId, PaymentAttemptId attempt) {
        return new ConfirmGatewayPaymentRequest(gatewayEventId, attempt.value(), CHECKOUT_ID, false,
                CHECKOUT_STARTED_AT, "CARD_DECLINED");
    }

    /** Aisyah's checkout, opened and waiting for a confirmed payment. */
    public static PaymentHold aisyahsPendingHold() {
        return PaymentHold.openForCheckout(new PaymentHoldId(HOLD_ID), CHECKOUT, AISYAH, TOTAL_RESERVATION_FEE);
    }

    /** Aisyah's RM200, paid by online banking and held. */
    public static PaymentHold aisyahsHeldPayment() {
        PaymentHold hold = aisyahsPendingHold();
        hold.recordConfirmation(ONLINE_BANKING, PAID_AT);
        return hold;
    }

    /** Aisyah's RM200, held and linked to her Requested reservation (RES-7). */
    public static PaymentHold aisyahsRequestedHold() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.linkToReservation(RESERVATION);
        return hold;
    }

    /** Kemas Home's RM200 JOB payout, opened at Reserved. */
    public static Payout kemasHomesPayoutAtReserved() {
        return Payout.openForReservation(new PayoutId(PAYOUT_ID), RESERVATION, KEMAS_HOME, TOTAL_RESERVATION_FEE,
                HomaFixtures.REQUESTED_AT, PROTECTION_WINDOW_END);
    }

    /** Kemas Home's RM200 JOB payout, the job marked Completed on Saturday 14:00. */
    public static Payout kemasHomesCompletedPayout() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.markCompleted(COMPLETED_AT);
        return payout;
    }
}
