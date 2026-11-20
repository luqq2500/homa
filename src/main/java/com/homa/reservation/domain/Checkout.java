package com.homa.reservation.domain;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Instant;
import java.util.Objects;

/**
 * A customer paying for a booking (RES-11). It is not a reservation and has no status: it holds the provider's and
 * the customer's time for 15 minutes, and carries the terms and the price the customer is paying.
 *
 * @param providerBlock the provider's time it holds: Check-in Period start to job end plus buffer (RES-9)
 * @param customerBlock the customer's time it holds: the job time (RES-10)
 */
public record Checkout(
        CheckoutId id,
        Booking booking,
        ReservationTerms terms,
        Quote quote,
        TimeInterval providerBlock,
        TimeInterval customerBlock,
        Instant holdExpiresAt) {

    public Checkout {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(booking, "booking");
        Objects.requireNonNull(terms, "terms");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(providerBlock, "providerBlock");
        Objects.requireNonNull(customerBlock, "customerBlock");
        Objects.requireNonNull(holdExpiresAt, "holdExpiresAt");
    }

    /**
     * Starts paying for a booking: the customer can reserve, the provider admits the booking (RES-1 to RES-5,
     * ACC-1, ACC-3), and both the provider's (RES-9) and the customer's (RES-10) time are free and are now held for
     * 15 minutes (RES-11).
     */
    public static Checkout start(
            CheckoutId id,
            Booking booking,
            BookableProvider provider,
            CustomerStanding customer,
            ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule,
            Instant now) {
        requireBookable(booking, provider, customer, now);
        TimeInterval providerBlock = provider.calendarBlockFor(booking.jobTime());
        TimeInterval customerBlock = booking.jobTime().interval();
        customerSchedule.requireFreeFor(id, customerBlock, now);
        Instant expiresAt = providerSchedule.hold(id, providerBlock, now);
        customerSchedule.hold(id, customerBlock, now);
        ReservationTerms terms = provider.termsFor(booking.address());
        Quote quote = ReservationPricing.quote(terms, booking.jobTime().duration(), booking.helpers());
        return new Checkout(id, booking, terms, quote, providerBlock, customerBlock, expiresAt);
    }

    static void requireBookable(Booking booking, BookableProvider provider, CustomerStanding customer, Instant now) {
        customer.requireCanReserve();
        provider.requireAdmits(booking, now);
    }

    /** ACC-1, ACC-3 under the terms being paid for: an Instant booking before check-in, a Request 1 hour before. */
    void requireRequestableAt(Instant paidAt) {
        if (!terms.acceptancePolicy().permitsNotice(booking.jobTime().noticeFrom(paidAt))) {
            throw new BookingRefusedException(BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
        }
    }
}
