package reservation.support;

import reservation.domain.ProviderStanding;
import reservation.usecase.port.out.ProviderStandingRepository;
import support.InMemoryStore;

public final class InMemoryProviderStandingRepository extends InMemoryStore<ProviderStanding> implements ProviderStandingRepository {

    public InMemoryProviderStandingRepository() {
        super(ProviderStanding::providerId);
    }
}
