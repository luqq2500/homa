package policy.usecase.port.out;

import java.util.List;
import policy.domain.ValueChange;

public interface ValueChangeRepository {

    void save(ValueChange change);

    /** Every change, oldest first. */
    List<ValueChange> all();
}
