package reservation.support;

import reservation.domain.ProviderTerms;
import reservation.usecase.port.out.ProviderTermsRepository;
import support.InMemoryStore;

public final class InMemoryProviderTermsRepository extends InMemoryStore<ProviderTerms> implements ProviderTermsRepository {

    public InMemoryProviderTermsRepository() {
        super(ProviderTerms::providerId);
    }
}
