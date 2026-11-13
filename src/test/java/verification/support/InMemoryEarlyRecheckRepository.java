package verification.support;

import support.InMemoryStore;
import verification.domain.EarlyRecheck;
import verification.usecase.port.out.EarlyRecheckRepository;

public final class InMemoryEarlyRecheckRepository extends InMemoryStore<EarlyRecheck> implements EarlyRecheckRepository {

    public InMemoryEarlyRecheckRepository() {
        super(EarlyRecheck::id);
    }
}
