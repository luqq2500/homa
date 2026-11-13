package offering.usecase;

import java.time.Clock;
import offering.domain.AcceptancePolicy;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.ChooseAcceptancePolicyRequest;
import offering.usecase.dto.ChooseAcceptancePolicyResponse;
import offering.usecase.port.in.ProviderAcceptancePolicyChooser;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderChooseAcceptancePolicy implements ProviderAcceptancePolicyChooser {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderChooseAcceptancePolicy(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChooseAcceptancePolicyResponse choose(ChooseAcceptancePolicyRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.chooseAcceptancePolicy(Offerings.choice(AcceptancePolicy.class, request.policy()));
        return new ChooseAcceptancePolicyResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
