package verification.usecase.port.out;

import java.util.List;
import java.util.Optional;
import verification.domain.EarlyRecheck;

public interface EarlyRecheckRepository {

    void save(EarlyRecheck earlyRecheck);

    Optional<EarlyRecheck> find(String id);

    List<EarlyRecheck> all();
}
