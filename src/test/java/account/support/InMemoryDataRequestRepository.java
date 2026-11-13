package account.support;

import account.domain.DataRequest;
import account.usecase.port.out.DataRequestRepository;
import support.InMemoryStore;

public final class InMemoryDataRequestRepository extends InMemoryStore<DataRequest> implements DataRequestRepository {

    public InMemoryDataRequestRepository() {
        super(DataRequest::id);
    }
}
