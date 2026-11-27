package com.homa.listing.usecase;

import com.homa.listing.domain.AcceptancePolicy;
import com.homa.listing.domain.BookingBounds;
import com.homa.listing.domain.CancellationTier;
import com.homa.listing.domain.JobBuffer;
import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.MinimumNotice;
import com.homa.listing.domain.ProviderListing;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesRequest;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesResponse;
import com.homa.listing.usecase.port.in.BookingRulesSetter;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.ProviderNotifier;
import java.time.Duration;
import java.util.OptionalInt;

/**
 * OFR-5, OFR-6, OFR-9, OFR-10: bounds, buffer, tier, policy, then notice. The policy comes before the notice, so a
 * notice is checked against the policy it will run under, and a provider switching to Request without giving a
 * notice has it raised to 2 hours and is told (OFR-6). Publishes the full terms, with the flag.
 */
public final class ProviderSetBookingRules implements BookingRulesSetter {

    private final ListingChanges changes;
    private final ProviderNotifier notifier;

    public ProviderSetBookingRules(ListingRepository listings, ListingOwners owners, ListingIEPublisher publisher,
                                   ListingEvents events, ProviderNotifier notifier) {
        this.changes = new ListingChanges(listings, owners, publisher, events);
        this.notifier = notifier;
    }

    @Override
    public ProviderSetBookingRulesResponse set(ProviderSetBookingRulesRequest request) {
        ListingTermsChange change = changes.apply(request.actingPersonId(), request.providerId(),
                listing -> apply(listing, request));
        Duration notice = change.terms().notice().duration();
        if (change.noticeRaised()) {
            notifier.noticeRaised(request.providerId(), notice);
        }
        return new ProviderSetBookingRulesResponse(request.providerId(), notice.toMinutes(), change.noticeRaised());
    }

    private static ListingTermsChange apply(ProviderListing listing, ProviderSetBookingRulesRequest request) {
        listing.setBookingBounds(bounds(request));
        listing.setJobBuffer(JobBuffer.of(Duration.ofMinutes(request.jobBufferMinutes())));
        listing.chooseCancellationTier(CancellationTier.valueOf(request.cancellationTier()));
        ListingTermsChange policyChange = listing.choosePolicy(AcceptancePolicy.valueOf(request.acceptancePolicy()));
        return request.minimumNoticeMinutes() == null
                ? policyChange
                : listing.setMinimumNotice(MinimumNotice.of(Duration.ofMinutes(request.minimumNoticeMinutes())));
    }

    private static BookingBounds bounds(ProviderSetBookingRulesRequest request) {
        OptionalInt maxHelpers = request.maxHelpers() == null ? OptionalInt.empty() : OptionalInt.of(request.maxHelpers());
        return new BookingBounds(Duration.ofMinutes(request.minBookingMinutes()),
                Duration.ofMinutes(request.maxBookingMinutes()), request.minHelpers(), maxHelpers);
    }
}
