package com.homa.reservation.usecase.ie;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.OfferedTerms;
import com.homa.reservation.domain.replica.ProviderListing;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import com.homa.reservation.usecase.port.in.ListingTermsChangedIEHandler;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import java.util.Optional;
import java.util.Set;

/**
 * OFR-1 to OFR-6, OFR-9, OFR-10: Listing published the provider's terms anew. Only the provider replica changes, so
 * only new bookings use them; existing reservations keep the terms they were Requested with (RES-8).
 */
public final class HandleListingTermsChangedIE implements ListingTermsChangedIEHandler {

    static final String HANDLER = "reservation.ListingTermsChanged";

    private final ProcessedMessageStore processed;
    private final BookableProviderRepository providers;

    public HandleListingTermsChangedIE(ProcessedMessageStore processed, BookableProviderRepository providers) {
        this.processed = processed;
        this.providers = providers;
    }

    @Override
    public void handle(ListingTermsChangedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ProviderListing listing = new ProviderListing(
                Set.copyOf(event.cities()),
                Set.copyOf(event.jobCodes()),
                new OfferedTerms(event.hourlyRatePerHelper(), travelFee(event), discount(event),
                        AcceptancePolicy.valueOf(event.acceptancePolicy()),
                        CancellationTier.valueOf(event.cancellationTier())),
                new BookingBounds(event.minBookingHours(), event.maxBookingHours(), event.bookingStep(),
                        event.minHelpers(), event.maxHelpers()),
                event.minimumNotice(),
                event.jobBuffer());
        providers.save(providers.findById(event.providerId())
                .orElseGet(() -> ProviderReplica.unheardOf(event.providerId()))
                .listedAs(listing));
    }

    /** OFR-3: none, flat, or by city, as the event names it. */
    private static TravelFeeRule travelFee(ListingTermsChangedIE event) {
        return switch (event.travelFeeKind()) {
            case "NONE" -> TravelFeeRule.none();
            case "FLAT" -> TravelFeeRule.flat(event.flatTravelFee());
            case "BY_CITY" -> TravelFeeRule.byCity(event.travelFeeByCity());
            default -> throw new IllegalArgumentException("Unknown travel fee kind " + event.travelFeeKind());
        };
    }

    /** OFR-4: no discount when the listing has none. */
    private static DiscountRule discount(ListingTermsChangedIE event) {
        return Optional.ofNullable(event.discountRate())
                .map(rate -> new DiscountRule(rate, event.discountMinHours(), event.discountMinHelpers()))
                .orElse(DiscountRule.NONE);
    }
}
