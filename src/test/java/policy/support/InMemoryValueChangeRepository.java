package policy.support;

import java.util.ArrayList;
import java.util.List;
import policy.domain.ValueChange;
import policy.usecase.port.out.ValueChangeRepository;

public final class InMemoryValueChangeRepository implements ValueChangeRepository {

    private final List<ValueChange> changes = new ArrayList<>();

    @Override
    public void save(ValueChange change) {
        changes.add(change);
    }

    @Override
    public List<ValueChange> all() {
        return List.copyOf(changes);
    }
}
