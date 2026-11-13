package policy.support;

import java.util.Optional;
import policy.domain.HomaValues;
import policy.usecase.port.out.HomaValuesRepository;

public final class InMemoryHomaValuesRepository implements HomaValuesRepository {

    private HomaValues current;

    @Override
    public Optional<HomaValues> current() {
        return Optional.ofNullable(current);
    }

    @Override
    public void save(HomaValues values) {
        current = values;
    }
}
