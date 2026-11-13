package offering.usecase;

import java.time.Clock;
import offering.domain.CancellationTier;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.ChooseCancellationTierRequest;
import offering.usecase.dto.ChooseCancellationTierResponse;
import offering.usecase.port.in.ProviderCancellationTierChooser;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderChooseCancellationTier implements ProviderCancellationTierChooser {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderChooseCancellationTier(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChooseCancellationTierResponse choose(ChooseCancellationTierRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.chooseCancellationTier(Offerings.choice(CancellationTier.class, request.tier()));
        return new ChooseCancellationTierResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
