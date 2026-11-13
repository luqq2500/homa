package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.ProviderTerms;

public interface ProviderTermsRepository {

    void save(ProviderTerms providerTerms);

    Optional<ProviderTerms> find(String id);

    List<ProviderTerms> all();
}
