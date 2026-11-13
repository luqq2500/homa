package offering.usecase.port.out;

import java.util.Optional;
import offering.domain.HoursRange;

/** Homa's duration range, as the policy context announced it. */
public interface HomaHoursRepository {

    Optional<HoursRange> current();

    void save(HoursRange homaRange);
}
