package catalogue.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.JobCode;
import catalogue.domain.JobGroup;
import catalogue.domain.JobList;
import catalogue.domain.PresetSearch;
import catalogue.domain.ServiceArea;
import catalogue.domain.Shelf;
import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.usecase.CatalogueFakes.InMemoryJobLists;
import catalogue.usecase.CatalogueFakes.InMemoryServiceAreas;
import catalogue.usecase.CatalogueFakes.InMemoryShelves;
import catalogue.usecase.CatalogueFakes.InMemoryToolLists;
import catalogue.usecase.dto.ViewJobListRequest;
import catalogue.usecase.dto.ViewJobListResponse;
import catalogue.usecase.dto.ViewServiceAreaRequest;
import catalogue.usecase.dto.ViewShelvesRequest;
import catalogue.usecase.dto.ViewShelvesResponse;
import catalogue.usecase.dto.ViewToolListRequest;
import catalogue.usecase.dto.ViewToolListResponse;
import catalogue.usecase.exception.ServiceAreaNotLaunchedException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** What providers and customers see of the catalogue. */
class CatalogueViewUsecasesTest {

    @Nested
    class ProviderViewsTheJobList {

        private final InMemoryJobLists jobLists = new InMemoryJobLists();
        private final ProviderViewJobList view = new ProviderViewJobList(jobLists);

        @Test
        void offeredJobsAreGroupedInTheOrderHomaAddedThem() {
            JobList list = JobList.create();
            list.add(JobCode.of("KITCHEN_GREASE"), "Degrease the kitchen", JobGroup.of("Kitchen"));
            list.add(JobCode.of("SOFA_VACUUM"), "Vacuum the sofa", JobGroup.of("Living room"));
            list.add(JobCode.of("KITCHEN_FLOOR"), "Mop the kitchen floor", JobGroup.of("Kitchen"));
            list.add(JobCode.of("WARDROBE_ORGANISE"), "Organise the wardrobe", JobGroup.of("Bedroom"));
            list.retire(JobCode.of("WARDROBE_ORGANISE"));
            jobLists.holding(list);

            ViewJobListResponse response = view.view(new ViewJobListRequest());

            assertThat(response.groups()).containsExactly(
                    new ViewJobListResponse.Group("Kitchen", List.of(
                            new ViewJobListResponse.Job("KITCHEN_GREASE", "Degrease the kitchen"),
                            new ViewJobListResponse.Job("KITCHEN_FLOOR", "Mop the kitchen floor"))),
                    new ViewJobListResponse.Group("Living room", List.of(
                            new ViewJobListResponse.Job("SOFA_VACUUM", "Vacuum the sofa"))));
        }

        @Test
        void noJobsAreShownBeforeHomaAddsAny() {
            assertThat(view.view(new ViewJobListRequest()).groups()).isEmpty();
        }
    }

    @Nested
    class ProviderViewsTheToolList {

        private final InMemoryToolLists toolLists = new InMemoryToolLists();
        private final ProviderViewToolList view = new ProviderViewToolList(toolLists);

        @Test
        void toolsThatCanBePickedAreShownInTheOrderHomaAddedThem() {
            ToolList list = ToolList.create();
            list.add(ToolCode.of("VACUUM"), "Vacuum");
            list.add(ToolCode.of("LADDER"), "Ladder");
            list.add(ToolCode.of("STEAM_MOP"), "Steam mop");
            list.retire(ToolCode.of("LADDER"));
            toolLists.holding(list);

            ViewToolListResponse response = view.view(new ViewToolListRequest());

            assertThat(response.tools()).containsExactly(
                    new ViewToolListResponse.Tool("VACUUM", "Vacuum"), new ViewToolListResponse.Tool("STEAM_MOP", "Steam mop"));
        }

        @Test
        void noToolsAreShownBeforeHomaAddsAny() {
            assertThat(view.view(new ViewToolListRequest()).tools()).isEmpty();
        }
    }

    @Nested
    class CustomerViewsTheShelves {

        private final InMemoryShelves shelves = new InMemoryShelves();
        private final CustomerViewShelves view = new CustomerViewShelves(shelves);

        @Test
        void shelvesAreShownInTheirPlaceOnTheHomepage() {
            Shelf second = Shelf.create("Deep clean", PresetSearch.of(Set.of(JobCode.of("TOILET_CARE")), 4, 2), 2);
            Shelf first = Shelf.create("Weekend quick refresh", PresetSearch.of(Set.of(JobCode.of("KITCHEN")), 2, 1), 1);
            shelves.save(second);
            shelves.save(first);

            ViewShelvesResponse response = view.view(new ViewShelvesRequest());

            assertThat(response.shelves()).containsExactly(
                    new ViewShelvesResponse.Shelf(first.id(), "Weekend quick refresh", 1, Set.of("KITCHEN"), 2, 1),
                    new ViewShelvesResponse.Shelf(second.id(), "Deep clean", 2, Set.of("TOILET_CARE"), 4, 2));
        }
    }

    @Nested
    class ProviderViewsTheServiceArea {

        private final InMemoryServiceAreas areas = new InMemoryServiceAreas();
        private final ProviderViewServiceArea view = new ProviderViewServiceArea(areas);

        @Test
        void theCitiesHomaServesAreShown() {
            areas.stored = ServiceArea.launch();

            assertThat(view.view(new ViewServiceAreaRequest()).cities())
                    .containsExactlyInAnyOrder("Salak Tinggi", "Cyberjaya", "Putrajaya");
        }

        @Test
        void noCitiesBeforeHomaLaunches() {
            assertThatThrownBy(() -> view.view(new ViewServiceAreaRequest())).isInstanceOf(ServiceAreaNotLaunchedException.class);
        }
    }
}
