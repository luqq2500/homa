package policy.usecase.port.out;

import java.util.Optional;
import policy.domain.HomaValues;

public interface HomaValuesRepository {

    /** The version in force, or nothing when no value has been changed since launch. */
    Optional<HomaValues> current();

    void save(HomaValues values);
}
