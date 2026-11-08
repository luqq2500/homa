package payment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import kernel.Money;

/** Test-data builders for payments, fines and payouts. */
final class PaymentBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    static final Instant NOW = at(MONDAY.plusDays(3), "09:00");

    private PaymentBuilders() {
    }

    static Instant at(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static Instant monday() {
        return at(MONDAY, "12:00");
    }

    static Instant tuesday() {
        return at(MONDAY.plusDays(1), "12:00");
    }

    static Instant saturday(String time) {
        return at(MONDAY.plusDays(5), time);
    }

    static Money rm(String amount) {
        return Money.parse(amount);
    }

    static Payment anAuthorisedCardPaymentOf(String total) {
        return Payment.authoriseCard("reservation-1", rm(total), NOW);
    }

    static Payment aHeldFpxPaymentOf(String total) {
        return Payment.payUpfrontByFpx("reservation-1", rm(total), NOW);
    }

    /** A card payment charged when the provider accepted. */
    static Payment aHeldPaymentOf(String total) {
        Payment payment = anAuthorisedCardPaymentOf(total);
        payment.capture(NOW);
        return payment;
    }

    static Settlement aSettlement(String earning, String fee, String refund) {
        return new Settlement("reservation-1", rm(earning), rm(fee), rm(refund), saturday("10:00"));
    }

    static Fine aFineOf(String amount, Instant chargedAt) {
        return Fine.charge("provider-1", new FineCharge("reservation-0", rm(amount), FineReason.NO_SHOW), chargedAt);
    }

    static Payout aPayoutOf(String earning) {
        return Payout.create("provider-1", "reservation-1", rm(earning), PayoutKind.COMPLETED_JOB);
    }
}
