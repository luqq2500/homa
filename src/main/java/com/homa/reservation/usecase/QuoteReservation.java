package com.homa.reservation.usecase;

import com.homa.reservation.domain.JobDuration;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.dto.QuoteReservationRequest;
import com.homa.reservation.usecase.dto.QuoteReservationResponse;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.port.in.ReservationQuoter;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;

/** RES-6, OFR-4: the price of a booking under the provider's terms today, for the city of the customer's home. */
public final class QuoteReservation implements ReservationQuoter {

    private final CustomerStandingRepository customers;
    private final BookableProviders providers;

    public QuoteReservation(CustomerStandingRepository customers, BookableProviders providers) {
        this.customers = customers;
        this.providers = providers;
    }

    @Override
    public QuoteReservationResponse quote(QuoteReservationRequest request) {
        CustomerStanding customer = customers.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        BookableProvider provider = providers.find(request.providerId());
        Quote quote = ReservationPricing.quote(provider.termsFor(customer.homeAddress()),
                JobDuration.ofMinutes(request.minutes()), request.helpers());
        return new QuoteReservationResponse(quote.hoursFee(), quote.travelFee(), quote.total(),
                quote.baseCancellationFee(), quote.discountRate().value());
    }
}
