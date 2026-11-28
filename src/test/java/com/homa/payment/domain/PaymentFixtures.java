package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import java.time.Instant;

/** The §0 example setup as Payment sees it: Aisyah's RM200 payment and Kemas Home's payout. */
final class PaymentFixtures {

    private PaymentFixtures() {
    }

    static final CustomerId AISYAH = new CustomerId(AISYAH_CUSTOMER_ID);
    static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);
    static final CheckoutId STANDARD_CHECKOUT = new CheckoutId("checkout-standard");
    static final ReservationId STANDARD_RESERVATION = new ReservationId("reservation-standard");
    static final PaymentHoldId AISYAHS_HOLD = new PaymentHoldId("hold-aisyah-standard");
    static final PayoutId KEMAS_HOMES_PAYOUT = new PayoutId("payout-kemas-home-standard");
    static final ReportId VASE_REPORT = new ReportId("report-cracked-vase");
    static final ReportId BATHROOM_REPORT = new ReportId("report-bathroom-not-cleaned");

    /** Kemas Home Sdn Bhd's company bank account, the payout account it submits (PRV-2, PRV-7). */
    static final PayoutAccountId KEMAS_HOMES_ACCOUNT = new PayoutAccountId("payout-account-kemas-home-maybank");
    static final BankAccount KEMAS_HOMES_BANK_ACCOUNT =
            new BankAccount("MAYBANK", "514011223344", HomaFixtures.KEMAS_HOME_NAME);
    /** The account Kemas Home submits after its first one fails the holder check. */
    static final PayoutAccountId KEMAS_HOMES_SECOND_ACCOUNT = new PayoutAccountId("payout-account-kemas-home-cimb");
    static final BankAccount KEMAS_HOMES_SECOND_BANK_ACCOUNT =
            new BankAccount("CIMB", "8001234567", HomaFixtures.KEMAS_HOME_NAME);
    /** Azman submits Kemas Home's payout account the week before it goes live. */
    static final Instant ACCOUNT_SUBMITTED_AT = at(2026, 11, 23, 10, 0);
    /** Verification's holder check comes back the same afternoon. */
    static final Instant ACCOUNT_CHECKED_AT = at(2026, 11, 23, 15, 0);

    /** Aisyah's online banking payment for the standard checkout. */
    static final PaymentAttemptId AISYAHS_ONLINE_BANKING = new PaymentAttemptId("attempt-aisyah-online-banking");
    /** Aisyah's card payment, retried after her first card attempt failed (RES-12). */
    static final PaymentAttemptId AISYAHS_CARD_RETRY = new PaymentAttemptId("attempt-aisyah-card-retry");

    /** Aisyah's online banking payment succeeds: Monday 20:00, when the reservation is Requested. */
    static final Instant PAID_AT = REQUESTED_AT;
    /** RES-12: Aisyah's card retry confirms at Monday 20:04. */
    static final Instant CARD_RETRY_CONFIRMED_AT = at(2026, 11, 30, 20, 4);
    /** RES-12: her slow online banking payment confirms after it, at Monday 20:06. */
    static final Instant SLOW_ONLINE_BANKING_CONFIRMED_AT = at(2026, 11, 30, 20, 6);
    /** Kemas Home accepts on Monday 22:00 and the reservation is Reserved (ACC-2). */
    static final Instant RESERVED_AT = at(2026, 11, 30, 22, 0);
    /** Aisyah cancels on Friday 22:00, after Standard's 21:45 cut-off (CCX-6). */
    static final Instant CANCELLED_AT = at(2026, 12, 4, 22, 0);
    /** Kemas Home marks the job Completed at the job end time, Saturday 14:00. */
    static final Instant MARKED_COMPLETED_AT = HomaFixtures.JOB_END;
    /** The Protection Window ends Sunday 14:00. */
    static final Instant PROTECTION_WINDOW_END = PROTECTION_WINDOW.end();

    /** Kemas Home's business payout account, waiting for Verification's holder check. */
    static PayoutAccount kemasHomesSubmittedAccount() {
        return PayoutAccount.submit(KEMAS_HOME, KEMAS_HOMES_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_BANK_ACCOUNT, ACCOUNT_SUBMITTED_AT);
    }

    /** Kemas Home's business payout account, its holder matched (PRV-2). */
    static PayoutAccount kemasHomesVerifiedAccount() {
        PayoutAccount account = kemasHomesSubmittedAccount();
        account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT);
        return account;
    }

    /** Kemas Home has never been fined: releasing a payout deducts nothing (PAY-5). */
    static ProviderFineAccount owingNothing() {
        return ProviderFineAccount.openFor(KEMAS_HOME);
    }

    /** Kemas Home owes the RM105 fine of a provider no-show (PAY-4). */
    static ProviderFineAccount owingTheNoShowFine() {
        ProviderFineAccount account = ProviderFineAccount.openFor(KEMAS_HOME);
        account.recordFine(new ReservationId("reservation-no-show"), Money.myr(105), at(2026, 12, 5, 10, 30));
        return account;
    }

    /** Aisyah has tapped Pay; the gateway has not confirmed any attempt yet. */
    static PaymentHold aisyahsCheckoutAwaitingPayment() {
        return PaymentHold.openForCheckout(AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, TOTAL_RESERVATION_FEE);
    }

    /** Aisyah's RM200, paid by online banking and held since Monday 20:00 (PAY-1). */
    static PaymentHold aisyahsHeldPayment() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();
        hold.recordConfirmation(AISYAHS_ONLINE_BANKING, PAID_AT);
        return hold;
    }

    /** Kemas Home's RM200 payout, opened when the reservation was Reserved on Monday; the job isn't done yet. */
    static Payout kemasHomesPayoutAtReserved() {
        return payoutAtReservedOf(TOTAL_RESERVATION_FEE, REQUESTED_AT);
    }

    static Payout payoutAtReservedOf(Money total, Instant requestedAt) {
        return Payout.openForReservation(KEMAS_HOMES_PAYOUT, STANDARD_RESERVATION, KEMAS_HOME, total, requestedAt,
                PROTECTION_WINDOW_END);
    }

    /** Kemas Home's RM200 job payout, the job marked Completed on Saturday 14:00. */
    static Payout kemasHomesJobPayout() {
        return jobPayoutOf(TOTAL_RESERVATION_FEE);
    }

    static Payout jobPayoutOf(Money gross) {
        Payout payout = payoutAtReservedOf(gross, REQUESTED_AT);
        payout.markCompleted(MARKED_COMPLETED_AT);
        return payout;
    }

    /** Aisyah cancelled on Friday 22:00 and pays Kemas Home the RM45 Base Cancellation Fee (CCX-6, PAY-6). */
    static Payout kemasHomesCancellationFeePayout() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.payFeeInstead(HomaFixtures.BASE_CANCELLATION_FEE);
        return payout;
    }
}
