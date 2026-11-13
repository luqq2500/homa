package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.ProviderStanding;

public interface ProviderStandingRepository {

    void save(ProviderStanding providerStanding);

    Optional<ProviderStanding> find(String id);

    List<ProviderStanding> all();
}
