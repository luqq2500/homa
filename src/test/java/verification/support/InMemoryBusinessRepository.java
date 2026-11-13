package verification.support;

import support.InMemoryStore;
import verification.domain.Business;
import verification.usecase.port.out.BusinessRepository;

public final class InMemoryBusinessRepository extends InMemoryStore<Business> implements BusinessRepository {

    public InMemoryBusinessRepository() {
        super(Business::id);
    }
}
