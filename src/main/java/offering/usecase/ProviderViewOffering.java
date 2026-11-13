package offering.usecase;

import ie.events.OfferingChangedIE;
import java.time.Clock;
import offering.domain.Missing;
import offering.domain.Offering;
import offering.domain.OfferingState;
import offering.usecase.dto.ViewOfferingRequest;
import offering.usecase.dto.ViewOfferingResponse;
import offering.usecase.port.in.ProviderOfferingViewer;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderViewOffering implements ProviderOfferingViewer {

    private final OfferingRepository offerings;
    private final Clock clock;

    public ProviderViewOffering(OfferingRepository offerings, Clock clock) {
        this.offerings = offerings;
        this.clock = clock;
    }

    @Override
    public ViewOfferingResponse view(ViewOfferingRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        OfferingState state = offering.state();
        OfferingChangedIE terms = Offerings.changed(state, clock.instant());
        return new ViewOfferingResponse(terms.listing(), Offerings.name(state.stoppedBecause()),
                offering.missingToGoLive().stream().map(Missing::name).toList(), terms.profileName(),
                terms.profilePicture(), terms.jobIds(), terms.toolIds(), terms.minHelpers(), terms.maxHelpers(),
                terms.supervisorFromHelpers(), terms.supervisorFromHours(), terms.hourlyRate(),
                terms.travelFeeSeparate(), terms.travelFee(), terms.minHours(), terms.maxHours(), terms.cities(),
                terms.cancellationTier(), terms.acceptancePolicy(), terms.insurer(), terms.insuredUntil(),
                state.team().helpers(), state.team().supervisors());
    }
}
