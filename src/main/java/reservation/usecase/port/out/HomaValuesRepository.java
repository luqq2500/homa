package reservation.usecase.port.out;

import java.util.Optional;
import reservation.domain.HomaValues;

/** The Homa-set values in force, as the policy context announced them. */
public interface HomaValuesRepository {

    Optional<HomaValues> current();

    void save(HomaValues values);
}
