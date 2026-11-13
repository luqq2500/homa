package verification.usecase.port.out;

import java.util.List;
import java.util.Optional;
import verification.domain.Business;

public interface BusinessRepository {

    void save(Business business);

    Optional<Business> find(String id);

    List<Business> all();
}
