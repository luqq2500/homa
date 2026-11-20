package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import java.time.Instant;

/** The §0 example setup as Payment sees it: Aisyah's RM200 payment and Kemas Home's payouts. */
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

    /** Aisyah's online banking payment succeeds: Monday 20:00, when the reservation is Requested. */
    static final Instant PAID_AT = REQUESTED_AT;
    /** Kemas Home marks the job Completed at the job end time, Saturday 14:00. */
    static final Instant MARKED_COMPLETED_AT = HomaFixtures.JOB_END;
    /** The Protection Window ends Sunday 14:00. */
    static final Instant PROTECTION_WINDOW_END = PROTECTION_WINDOW.end();

    /** Aisyah has tapped Pay; the gateway has not confirmed yet. */
    static PaymentHold aisyahsCheckoutAwaitingPayment() {
        return PaymentHold.openForCheckout(AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, TOTAL_RESERVATION_FEE);
    }

    /** Aisyah's RM200, held since Monday 20:00 (PAY-1). */
    static PaymentHold aisyahsHeldPayment() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();
        hold.hold(PAID_AT);
        return hold;
    }

    /** Kemas Home's RM200 payout for the standard job, marked Completed on Saturday. */
    static Payout kemasHomesJobPayout() {
        return jobPayoutOf(TOTAL_RESERVATION_FEE);
    }

    static Payout jobPayoutOf(Money gross) {
        return Payout.forCompletedJob(KEMAS_HOMES_PAYOUT, STANDARD_RESERVATION, KEMAS_HOME, gross,
                PROTECTION_WINDOW_END);
    }

    /** The RM45 cancellation fee Aisyah paid Kemas Home, had she cancelled on Friday 22:00 (PAY-3, PAY-6). */
    static Payout kemasHomesCancellationFeePayout() {
        return Payout.forFee(KEMAS_HOMES_PAYOUT, STANDARD_RESERVATION, KEMAS_HOME,
                HomaFixtures.BASE_CANCELLATION_FEE, PROTECTION_WINDOW_END);
    }
}
