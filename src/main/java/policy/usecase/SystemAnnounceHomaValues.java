package policy.usecase;

import java.time.Clock;
import policy.domain.HomaValues;
import policy.usecase.dto.AnnounceHomaValuesResponse;
import policy.usecase.port.in.HomaValuesAnnouncer;
import policy.usecase.port.out.HomaValuesRepository;
import policy.usecase.port.out.PolicyIEPublisher;

public final class SystemAnnounceHomaValues implements HomaValuesAnnouncer {

    private final HomaValuesRepository values;
    private final PolicyIEPublisher publisher;
    private final Clock clock;

    public SystemAnnounceHomaValues(HomaValuesRepository values, PolicyIEPublisher publisher, Clock clock) {
        this.values = values;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public AnnounceHomaValuesResponse announce() {
        HomaValues current = values.current().orElseGet(HomaValues::launch);
        publisher.publish(HomaValuesEvents.valuesChanged(current, clock.instant()));
        return new AnnounceHomaValuesResponse(current.version());
    }
}
