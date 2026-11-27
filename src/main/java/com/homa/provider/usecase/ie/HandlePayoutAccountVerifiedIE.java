package com.homa.provider.usecase.ie;

import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.Team;
import com.homa.provider.usecase.port.in.PayoutAccountVerifiedIEHandler;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import java.util.Objects;

/**
 * Payment reports whether the provider's payout account passed the holder check. A verified one, held by the owner or
 * the business, lets the provider go live and be booked; without one it can't be booked (PRV-2). When a live
 * provider's bookability changes, {@code ProviderBookabilityChangedIE} tells Listing, Reservation and Notification. A
 * provider that hasn't gone live stays NOT_LIVE either way, so nothing is published for it.
 *
 * <p>An event for a provider Provider doesn't know names a payout account no provider holds. It is recorded as handled
 * and dropped, so the relay doesn't retry it forever.
 *
 * <p>Left for slice 6: Business status from an account held by the business (PRV-7), and its loss (PRV-9).
 */
public final class HandlePayoutAccountVerifiedIE implements PayoutAccountVerifiedIEHandler {

    static final String HANDLER = "provider.PayoutAccountVerified";

    private final ProcessedMessageStore processedMessages;
    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public HandlePayoutAccountVerifiedIE(ProcessedMessageStore processedMessages, ProviderRepository providers,
            TeamMemberRepository members, ProviderIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
    }

    @Override
    public void handle(PayoutAccountVerifiedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        providers.findById(new ProviderId(event.providerId())).ifPresent(provider -> apply(event, provider));
    }

    private void apply(PayoutAccountVerifiedIE event, Provider provider) {
        int activeHelpers = new Team(members.findByProviderId(provider.id())).activeHelperCount();
        Bookability before = provider.bookabilityFor(activeHelpers);
        if (event.verified()) {
            provider.recordPayoutAccountVerified(PayoutAccountHolder.valueOf(event.holder()));
        } else {
            provider.recordPayoutAccountNotVerified();
        }
        providers.save(provider);
        Bookability after = provider.bookabilityFor(activeHelpers);
        if (after != before) {
            publisher.publish(new ProviderBookabilityChangedIE(eventIds.next(), provider.id().value(),
                    after.isBookable(), after.isBookable() ? null : after.name(), time.now()));
        }
    }
}
