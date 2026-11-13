package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetSupervisorRuleRequest;
import offering.usecase.dto.SetSupervisorRuleResponse;
import offering.usecase.port.in.ProviderSupervisorRuleSetter;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetSupervisorRule implements ProviderSupervisorRuleSetter {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetSupervisorRule(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetSupervisorRuleResponse set(SetSupervisorRuleRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setSupervisorRule(request.fromHelpers(), request.fromHours());
        return new SetSupervisorRuleResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
