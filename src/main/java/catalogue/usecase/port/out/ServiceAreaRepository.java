package catalogue.usecase.port.out;

import catalogue.domain.ServiceArea;
import java.util.Optional;

/** Keeps the cities Homa serves. */
public interface ServiceAreaRepository {

    /** The service area, or empty before it is published. */
    Optional<ServiceArea> find();

    void save(ServiceArea serviceArea);
}
