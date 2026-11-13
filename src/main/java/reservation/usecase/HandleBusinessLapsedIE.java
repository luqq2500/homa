package reservation.usecase;

import ie.events.BusinessLapsedIE;
import reservation.domain.ProviderStanding;
import reservation.usecase.port.in.BusinessLapsedIEHandler;
import reservation.usecase.port.out.ProviderStandingRepository;

public final class HandleBusinessLapsedIE implements BusinessLapsedIEHandler {

    private final ProviderStandingRepository standings;

    public HandleBusinessLapsedIE(ProviderStandingRepository standings) {
        this.standings = standings;
    }

    @Override
    public void handle(BusinessLapsedIE event) {
        ProviderStanding standing = standings.find(event.businessId())
                .orElseGet(() -> ProviderStanding.unverified(event.businessId()));
        standings.save(new ProviderStanding(standing.providerId(), null, standing.seriouslyFlagged()));
    }
}
