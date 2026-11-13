package reservation.usecase;

import ie.events.BusinessVerifiedIE;
import reservation.domain.ProviderStanding;
import reservation.usecase.port.in.BusinessVerifiedIEHandler;
import reservation.usecase.port.out.ProviderStandingRepository;

public final class HandleBusinessVerifiedIE implements BusinessVerifiedIEHandler {

    private final ProviderStandingRepository standings;

    public HandleBusinessVerifiedIE(ProviderStandingRepository standings) {
        this.standings = standings;
    }

    @Override
    public void handle(BusinessVerifiedIE event) {
        ProviderStanding standing = standings.find(event.businessId())
                .orElseGet(() -> ProviderStanding.unverified(event.businessId()));
        standings.save(new ProviderStanding(standing.providerId(), event.verifiedUntil(), standing.seriouslyFlagged()));
    }
}
