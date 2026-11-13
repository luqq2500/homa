package reservation.usecase;

import ie.events.BusinessFlagClearedIE;
import reservation.domain.ProviderStanding;
import reservation.usecase.port.in.BusinessFlagClearedIEHandler;
import reservation.usecase.port.out.ProviderStandingRepository;

public final class HandleBusinessFlagClearedIE implements BusinessFlagClearedIEHandler {

    private final ProviderStandingRepository standings;

    public HandleBusinessFlagClearedIE(ProviderStandingRepository standings) {
        this.standings = standings;
    }

    @Override
    public void handle(BusinessFlagClearedIE event) {
        ProviderStanding standing = standings.find(event.businessId())
                .orElseGet(() -> ProviderStanding.unverified(event.businessId()));
        standings.save(new ProviderStanding(standing.providerId(), standing.verifiedUntil(), false));
    }
}
