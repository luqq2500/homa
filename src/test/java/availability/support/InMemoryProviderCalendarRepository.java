package availability.support;

import availability.domain.ProviderCalendar;
import availability.usecase.port.out.ProviderCalendarRepository;
import support.InMemoryStore;

public final class InMemoryProviderCalendarRepository extends InMemoryStore<ProviderCalendar> implements ProviderCalendarRepository {

    public InMemoryProviderCalendarRepository() {
        super(ProviderCalendar::providerId);
    }
}
