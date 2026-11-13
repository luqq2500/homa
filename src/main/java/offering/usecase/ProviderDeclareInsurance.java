package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.DeclareInsuranceRequest;
import offering.usecase.dto.DeclareInsuranceResponse;
import offering.usecase.port.in.ProviderInsuranceDeclarer;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderDeclareInsurance implements ProviderInsuranceDeclarer {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderDeclareInsurance(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public DeclareInsuranceResponse declare(DeclareInsuranceRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.declareInsurance(request.insurer(), request.validUntil());
        return new DeclareInsuranceResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
