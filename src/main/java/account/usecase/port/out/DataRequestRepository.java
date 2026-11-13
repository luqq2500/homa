package account.usecase.port.out;

import account.domain.DataRequest;
import java.util.List;
import java.util.Optional;

public interface DataRequestRepository {

    void save(DataRequest dataRequest);

    Optional<DataRequest> find(String id);

    List<DataRequest> all();
}
