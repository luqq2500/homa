package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.NOW;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryServiceAreas;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.PublishServiceAreaRequest;
import catalogue.usecase.dto.PublishServiceAreaResponse;
import catalogue.usecase.exception.DecidedByRequiredException;
import catalogue.usecase.exception.ServiceAreaNotPublishedException;
import ie.events.ServiceAreaPublishedIE;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Homa's founders publish the cities Homa serves, and providers see the cities they can cover. */
class HomaServiceAreaPublicationTest {

    private final InMemoryServiceAreas serviceAreas = new InMemoryServiceAreas();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();
    private final HomaPublishServiceArea publishServiceArea = new HomaPublishServiceArea(serviceAreas, events, auditLog, CLOCK);
    private final ProviderViewServiceArea viewServiceArea = new ProviderViewServiceArea(serviceAreas);

    @Test
    void theFirstPublicationSetsUpTheLaunchCities() {
        PublishServiceAreaResponse published = publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));

        assertThat(published.cities()).containsExactly("Cyberjaya", "Putrajaya", "Salak Tinggi");
        assertThat(serviceAreas.saves).isEqualTo(1);
        assertThat(serviceAreas.find()).isPresent();
    }

    @Test
    void thePublishedCitiesAreToldToTheOtherContexts() {
        publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));

        assertThat(events.all)
                .containsExactly(new ServiceAreaPublishedIE(Set.of("Salak Tinggi", "Cyberjaya", "Putrajaya"), NOW));
    }

    @Test
    void aPublicationIsRecordedWithWhoPublishedItAndWhen() {
        publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));

        assertThat(auditLog.all).containsExactly(recorded("PUBLISH_SERVICE_AREA", serviceAreas.find().orElseThrow().id()));
    }

    @Test
    void publishingAgainTellsTheSameServiceAreaAgain() {
        publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));
        String areaId = serviceAreas.find().orElseThrow().id();

        publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));

        assertThat(serviceAreas.find().orElseThrow().id()).isEqualTo(areaId);
        assertThat(events.all).hasSize(2);
        assertThat(events.all.get(1)).isEqualTo(events.all.get(0));
        assertThat(auditLog.all).hasSize(2);
    }

    @Test
    void publishingTheServiceAreaNeedsTheFounderWhoDecidedIt() {
        assertThatThrownBy(() -> publishServiceArea.publish(new PublishServiceAreaRequest(null)))
                .isInstanceOf(DecidedByRequiredException.class);

        assertThat(serviceAreas.find()).isEmpty();
        assertThat(events.all).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void aProviderSeesTheCitiesTheyCanCoverInAlphabeticalOrder() {
        publishServiceArea.publish(new PublishServiceAreaRequest(FOUNDER));

        assertThat(viewServiceArea.view().cities()).containsExactly("Cyberjaya", "Putrajaya", "Salak Tinggi");
    }

    @Test
    void aProviderCannotSeeTheServiceAreaBeforeItIsPublished() {
        assertThatThrownBy(viewServiceArea::view)
                .isInstanceOf(ServiceAreaNotPublishedException.class)
                .hasMessage("The service area has not been published yet");
    }
}
