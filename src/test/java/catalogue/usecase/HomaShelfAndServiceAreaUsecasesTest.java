package catalogue.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.City;
import catalogue.domain.JobCode;
import catalogue.domain.ServiceArea;
import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.usecase.CatalogueFakes.InMemoryServiceAreas;
import catalogue.usecase.CatalogueFakes.InMemoryShelves;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;
import catalogue.usecase.dto.LaunchServiceAreaRequest;
import catalogue.usecase.dto.LaunchServiceAreaResponse;
import catalogue.usecase.exception.ServiceAreaAlreadyLaunchedException;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Homa curates the homepage shelves and launches its service area. */
class HomaShelfAndServiceAreaUsecasesTest {

    @Nested
    class HomaCreatesAShelf {

        private final InMemoryShelves shelves = new InMemoryShelves();
        private final HomaCreateShelf createShelf = new HomaCreateShelf(shelves);

        @Test
        void aShelfIsSavedWithItsPresetSearch() {
            CreateShelfResponse response = createShelf.create(
                    new CreateShelfRequest("Weekend quick refresh", Set.of("KITCHEN", "LIVING_ROOM_TIDY"), 2, 1, 1));

            assertThat(shelves.stored).hasSize(1);
            var saved = shelves.stored.get(0);
            assertThat(response).isEqualTo(new CreateShelfResponse(saved.id(), "Weekend quick refresh", 1));
            assertThat(saved.preset().jobs()).containsExactlyInAnyOrder(JobCode.of("KITCHEN"), JobCode.of("LIVING_ROOM_TIDY"));
            assertThat(saved.preset().hours()).isEqualTo(2);
            assertThat(saved.preset().helpers()).isEqualTo(1);
        }

        @Test
        void aShelfWithNoJobsIsNotSaved() {
            assertThatThrownBy(() -> createShelf.create(new CreateShelfRequest("Weekend quick refresh", Set.of(), 2, 1, 1)))
                    .isInstanceOf(EmptyPresetSearchException.class);
            assertThat(shelves.stored).isEmpty();
        }
    }

    @Nested
    class HomaLaunchesTheServiceArea {

        private final InMemoryServiceAreas areas = new InMemoryServiceAreas();
        private final HomaLaunchServiceArea launch = new HomaLaunchServiceArea(areas);

        @Test
        void theLaunchCitiesAreSaved() {
            LaunchServiceAreaResponse response = launch.launch(new LaunchServiceAreaRequest());

            assertThat(response.cities()).containsExactlyInAnyOrder("Salak Tinggi", "Cyberjaya", "Putrajaya");
            assertThat(areas.saves).isEqualTo(1);
            assertThat(areas.stored.covers(City.of("Cyberjaya"))).isTrue();
        }

        @Test
        void theServiceAreaIsLaunchedOnce() {
            areas.stored = ServiceArea.launch();

            assertThatThrownBy(() -> launch.launch(new LaunchServiceAreaRequest()))
                    .isInstanceOf(ServiceAreaAlreadyLaunchedException.class)
                    .hasMessage("Homa's service area is already launched");
            assertThat(areas.saves).isZero();
        }
    }
}
