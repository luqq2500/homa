package catalogue.usecase.port.out;

import catalogue.domain.ServiceArea;
import java.util.Optional;

/** Where the cities Homa serves are kept. */
public interface ServiceAreaRepository {

    /** The service area, or empty before Homa launches it. */
    Optional<ServiceArea> load();

    void save(ServiceArea area);
}
