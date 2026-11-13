package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.ProviderOpenTime;

public interface ProviderOpenTimeRepository {

    void save(ProviderOpenTime providerOpenTime);

    Optional<ProviderOpenTime> find(String id);

    List<ProviderOpenTime> all();
}
