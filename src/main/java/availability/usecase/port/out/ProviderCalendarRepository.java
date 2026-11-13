package availability.usecase.port.out;

import availability.domain.ProviderCalendar;
import java.util.List;
import java.util.Optional;

public interface ProviderCalendarRepository {

    void save(ProviderCalendar providerCalendar);

    Optional<ProviderCalendar> find(String id);

    List<ProviderCalendar> all();
}
