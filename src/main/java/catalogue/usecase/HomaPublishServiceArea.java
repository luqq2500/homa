package catalogue.usecase;

import catalogue.domain.City;
import catalogue.domain.ServiceArea;
import catalogue.usecase.dto.PublishServiceAreaRequest;
import catalogue.usecase.dto.PublishServiceAreaResponse;
import catalogue.usecase.port.in.HomaServiceAreaPublisher;
import catalogue.usecase.port.out.AdminAuditLog;
import catalogue.usecase.port.out.CatalogueIEPublisher;
import catalogue.usecase.port.out.ServiceAreaRepository;
import ie.events.ServiceAreaPublishedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Publishes the cities Homa serves, so providers can cover them. The first time, it sets up the launch
 * area; after that it publishes the same area again, so a context that missed it catches up.
 */
public final class HomaPublishServiceArea implements HomaServiceAreaPublisher {

    private final ServiceAreaRepository serviceAreas;
    private final CatalogueIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaPublishServiceArea(
            ServiceAreaRepository serviceAreas, CatalogueIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.serviceAreas = serviceAreas;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public PublishServiceAreaResponse publish(PublishServiceAreaRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        Instant now = clock.instant();
        ServiceArea area = serviceAreas.find().orElseGet(ServiceArea::launch);
        List<String> cities = area.cities().stream().map(City::name).sorted().toList();

        serviceAreas.save(area);
        events.publish(new ServiceAreaPublishedIE(Set.copyOf(cities), now));
        admin.record(founder, "PUBLISH_SERVICE_AREA", area.id(), now);
        return new PublishServiceAreaResponse(cities);
    }
}
