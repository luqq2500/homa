package reservation.usecase;

import ie.events.OfferingChangedIE;
import java.util.Optional;
import reservation.domain.AcceptancePolicy;
import reservation.domain.CancellationTier;
import reservation.domain.ProviderTerms;
import reservation.domain.SupervisorRule;
import reservation.usecase.port.in.OfferingChangedIEHandler;
import reservation.usecase.port.out.ProviderTermsRepository;

public final class HandleOfferingChangedIE implements OfferingChangedIEHandler {

    private final ProviderTermsRepository providerTerms;
    private static final String LIVE = "LIVE";

    public HandleOfferingChangedIE(ProviderTermsRepository providerTerms) {
        this.providerTerms = providerTerms;
    }

    @Override
    public void handle(OfferingChangedIE event) {
        providerTerms.save(new ProviderTerms(event.providerId(), LIVE.equals(event.listing()), event.jobIds(),
                event.minHelpers(), event.maxHelpers(),
                new SupervisorRule(event.supervisorFromHelpers(), event.supervisorFromHours()), event.hourlyRate(),
                event.travelFeeSeparate(), event.travelFee(), event.minHours(), event.maxHours(), event.cities(),
                Optional.ofNullable(event.cancellationTier()).map(CancellationTier::valueOf).orElse(null),
                AcceptancePolicy.valueOf(event.acceptancePolicy())));
    }
}
