package catalogue.usecase.port.out;

import catalogue.domain.ServiceArea;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the cities Homa serves.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface ServiceAreaRepository {

    /** The service area, or empty before it is published. */
    Optional<ServiceArea> find();

    void save(ServiceArea serviceArea);
}
