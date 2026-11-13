package reservation.support;

import reservation.domain.ProviderOpenTime;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import support.InMemoryStore;

public final class InMemoryProviderOpenTimeRepository extends InMemoryStore<ProviderOpenTime> implements ProviderOpenTimeRepository {

    public InMemoryProviderOpenTimeRepository() {
        super(ProviderOpenTime::providerId);
    }
}
