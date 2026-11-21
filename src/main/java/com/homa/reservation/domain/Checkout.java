package com.homa.reservation.domain;

import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import java.time.Instant;
import java.util.Objects;

/**
 * A customer paying for a booking. It is not a reservation, has no status and holds nothing (P1, RES-11): the time
 * was free when the customer started paying, but the first confirmed payment for a time wins. It carries the booking
 * and the price the customer pays; the terms are frozen only when the reservation is Requested (RES-8).
 */
public record Checkout(CheckoutId id, Booking booking, Quote quote) {

    public Checkout {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(booking, "booking");
        Objects.requireNonNull(quote, "quote");
    }

    /**
     * Starts paying for a booking at the customer's home address. It checks, without holding anything, that the
     * customer can reserve, the provider admits the booking (RES-1 to RES-5, ACC-1, ACC-3) and both the provider's
     * (RES-9) and the customer's (RES-10) time are free now, and prices it under the provider's terms (RES-6).
     */
    public static Checkout start(
            CheckoutId id,
            Booking booking,
            BookableProvider provider,
            CustomerStanding customer,
            ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule,
            Instant now) {
        Booking atHome = booking.at(customer.homeAddress());
        requireBookable(atHome, provider, customer, providerSchedule, customerSchedule, now);
        Quote quote = ReservationPricing.quote(
                provider.termsFor(atHome.address()), atHome.jobTime().duration(), atHome.helpers());
        return new Checkout(id, atHome, quote);
    }

    /** Every booking rule passes at this moment, and both parties' time is free. */
    static void requireBookable(
            Booking booking,
            BookableProvider provider,
            CustomerStanding customer,
            ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule,
            Instant at) {
        customer.requireCanReserve();
        provider.requireAdmits(booking, at);
        providerSchedule.requireFree(provider.calendarBlockFor(booking.jobTime(), ReservationValues.asOf(at)));
        customerSchedule.requireFree(booking.jobTime().interval());
    }
}
