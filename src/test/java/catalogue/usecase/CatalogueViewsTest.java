package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static org.assertj.core.api.Assertions.assertThat;

import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryHomepages;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryJobLists;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShelves;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryToolLists;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CustomerViewShelvesResponse;
import catalogue.usecase.dto.HomaViewJobListResponse;
import catalogue.usecase.dto.HomaViewShelvesResponse;
import catalogue.usecase.dto.HomaViewToolListResponse;
import catalogue.usecase.dto.ProviderViewJobListResponse;
import catalogue.usecase.dto.ProviderViewToolListResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireToolRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** What founders, providers and customers see of the job list, the tool list and the shelves. */
class CatalogueViewsTest {

    private final InMemoryJobLists jobLists = new InMemoryJobLists();
    private final InMemoryToolLists toolLists = new InMemoryToolLists();
    private final InMemoryShelves shelves = new InMemoryShelves();
    private final InMemoryHomepages homepages = new InMemoryHomepages();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();

    private void jobIsOnTheList(String code, String name, String group) {
        new HomaAddJob(jobLists, events, auditLog, CLOCK).add(new AddJobRequest(FOUNDER, code, name, group));
    }

    private void jobIsRetired(String code) {
        new HomaRetireJob(jobLists, shelves, events, auditLog, CLOCK).retire(new RetireJobRequest(FOUNDER, code, "No longer offered"));
    }

    private void toolIsOnTheList(String code, String name) {
        new HomaAddTool(toolLists, events, auditLog, CLOCK).add(new AddToolRequest(FOUNDER, code, name));
    }

    private void toolIsRetired(String code) {
        new HomaRetireTool(toolLists, events, auditLog, CLOCK).retire(new RetireToolRequest(FOUNDER, code, "No longer brought"));
    }

    private String shelfIsCreated(String title, int order, String... jobCodes) {
        return new HomaCreateShelf(shelves, homepages, jobLists, auditLog, CLOCK)
                .create(new CreateShelfRequest(FOUNDER, title, Set.of(jobCodes), 2, 1, order))
                .shelfId();
    }

    @Nested
    class TheJobList {

        private final HomaViewJobList foundersView = new HomaViewJobList(jobLists);
        private final ProviderViewJobList providersView = new ProviderViewJobList(jobLists);

        @Test
        void beforeAnyJobIsAddedBothViewsAreEmpty() {
            assertThat(foundersView.view().jobs()).isEmpty();
            assertThat(providersView.view().groups()).isEmpty();
        }

        @Test
        void theFoundersSeeEveryJobInTheOrderAddedRetiredOnesIncluded() {
            jobIsOnTheList("KITCHEN", "Kitchen care", "Kitchen");
            jobIsOnTheList("LAUNDRY_FOLD", "Laundry folding", "Laundry");
            jobIsRetired("KITCHEN");

            assertThat(foundersView.view().jobs()).containsExactly(
                    new HomaViewJobListResponse.Job("KITCHEN", "Kitchen care", "Kitchen", false),
                    new HomaViewJobListResponse.Job("LAUNDRY_FOLD", "Laundry folding", "Laundry", true));
        }

        @Test
        void aProviderSeesTheJobsTheyCanOfferByGroupInTheOrderAdded() {
            jobIsOnTheList("KITCHEN", "Kitchen care", "Kitchen");
            jobIsOnTheList("LAUNDRY_FOLD", "Laundry folding", "Laundry");
            jobIsOnTheList("KITCHEN_GREASE", "Kitchen grease removal", "Kitchen");

            assertThat(providersView.view().groups()).containsExactly(
                    new ProviderViewJobListResponse.Group("Kitchen", List.of(
                            new ProviderViewJobListResponse.Job("KITCHEN", "Kitchen care"),
                            new ProviderViewJobListResponse.Job("KITCHEN_GREASE", "Kitchen grease removal"))),
                    new ProviderViewJobListResponse.Group("Laundry", List.of(
                            new ProviderViewJobListResponse.Job("LAUNDRY_FOLD", "Laundry folding"))));
        }

        @Test
        void aProviderDoesNotSeeARetiredJobOrAGroupLeftWithNoJob() {
            jobIsOnTheList("KITCHEN", "Kitchen care", "Kitchen");
            jobIsOnTheList("LAUNDRY_FOLD", "Laundry folding", "Laundry");
            jobIsRetired("LAUNDRY_FOLD");

            List<ProviderViewJobListResponse.Group> groups = providersView.view().groups();

            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).name()).isEqualTo("Kitchen");
            assertThat(groups.get(0).jobs()).extracting(ProviderViewJobListResponse.Job::code).containsExactly("KITCHEN");
            assertThat(groups.get(0).jobs()).extracting(ProviderViewJobListResponse.Job::name).containsExactly("Kitchen care");
        }

        @Test
        void theFoundersSeeEachJobsCodeNameGroupAndWhetherItIsOffered() {
            jobIsOnTheList("KITCHEN", "Kitchen care", "Kitchen");

            HomaViewJobListResponse.Job job = foundersView.view().jobs().get(0);

            assertThat(job.code()).isEqualTo("KITCHEN");
            assertThat(job.name()).isEqualTo("Kitchen care");
            assertThat(job.group()).isEqualTo("Kitchen");
            assertThat(job.active()).isTrue();
        }
    }

    @Nested
    class TheToolList {

        private final HomaViewToolList foundersView = new HomaViewToolList(toolLists);
        private final ProviderViewToolList providersView = new ProviderViewToolList(toolLists);

        @Test
        void beforeAnyToolIsAddedBothViewsAreEmpty() {
            assertThat(foundersView.view().tools()).isEmpty();
            assertThat(providersView.view().tools()).isEmpty();
        }

        @Test
        void theFoundersSeeEveryToolInTheOrderAddedRetiredOnesIncluded() {
            toolIsOnTheList("STEAM_MOP", "Steam mop");
            toolIsOnTheList("VACUUM", "Vacuum cleaner");
            toolIsRetired("STEAM_MOP");

            List<HomaViewToolListResponse.Tool> tools = foundersView.view().tools();

            assertThat(tools).containsExactly(
                    new HomaViewToolListResponse.Tool("STEAM_MOP", "Steam mop", false),
                    new HomaViewToolListResponse.Tool("VACUUM", "Vacuum cleaner", true));
            assertThat(tools.get(1).code()).isEqualTo("VACUUM");
            assertThat(tools.get(1).name()).isEqualTo("Vacuum cleaner");
            assertThat(tools.get(1).active()).isTrue();
            assertThat(tools.get(0).active()).isFalse();
        }

        @Test
        void aProviderSeesOnlyTheToolsTheyCanSayTheyBring() {
            toolIsOnTheList("STEAM_MOP", "Steam mop");
            toolIsOnTheList("VACUUM", "Vacuum cleaner");
            toolIsOnTheList("LADDER", "Ladder");
            toolIsRetired("VACUUM");

            List<ProviderViewToolListResponse.Tool> tools = providersView.view().tools();

            assertThat(tools).containsExactly(
                    new ProviderViewToolListResponse.Tool("STEAM_MOP", "Steam mop"),
                    new ProviderViewToolListResponse.Tool("LADDER", "Ladder"));
            assertThat(tools.get(0).code()).isEqualTo("STEAM_MOP");
            assertThat(tools.get(0).name()).isEqualTo("Steam mop");
        }
    }

    @Nested
    class TheShelves {

        private final CustomerViewShelves customersView = new CustomerViewShelves(shelves, homepages);
        private final HomaViewShelves foundersView = new HomaViewShelves(shelves, homepages);

        TheShelves() {
            jobIsOnTheList("KITCHEN", "Kitchen care", "Kitchen");
            jobIsOnTheList("LAUNDRY_FOLD", "Laundry folding", "Laundry");
        }

        @Test
        void aCustomerDoesNotSeeAShelfLeftWithNoJob() {
            shelfIsCreated("Kitchen rescue", 1, "KITCHEN");
            String laundry = shelfIsCreated("Laundry day", 2, "LAUNDRY_FOLD");

            jobIsRetired("KITCHEN");

            assertThat(customersView.view().shelves()).extracting(CustomerViewShelvesResponse.Shelf::shelfId)
                    .containsExactly(laundry);
        }

        @Test
        void theFoundersSeeEveryShelfInOrderAndWhichAreNoLongerShown() {
            String laundry = shelfIsCreated("Laundry day", 1, "LAUNDRY_FOLD");
            String kitchen = shelfIsCreated("Kitchen rescue", 1, "KITCHEN");
            jobIsRetired("KITCHEN");

            List<HomaViewShelvesResponse.Shelf> all = foundersView.view().shelves();

            assertThat(all).containsExactly(
                    new HomaViewShelvesResponse.Shelf(kitchen, "Kitchen rescue", 1, Set.of("KITCHEN"), 2, 1, false),
                    new HomaViewShelvesResponse.Shelf(laundry, "Laundry day", 2, Set.of("LAUNDRY_FOLD"), 2, 1, true));
            HomaViewShelvesResponse.Shelf shown = all.get(1);
            assertThat(shown.shelfId()).isEqualTo(laundry);
            assertThat(shown.title()).isEqualTo("Laundry day");
            assertThat(shown.order()).isEqualTo(2);
            assertThat(shown.jobCodes()).containsExactly("LAUNDRY_FOLD");
            assertThat(shown.hours()).isEqualTo(2);
            assertThat(shown.helpers()).isEqualTo(1);
            assertThat(shown.shown()).isTrue();
            assertThat(all.get(0).shown()).isFalse();
        }

        @Test
        void beforeAnyShelfIsCreatedTheHomepageHasNone() {
            assertThat(customersView.view().shelves()).isEmpty();
            assertThat(foundersView.view().shelves()).isEmpty();
        }

        @Test
        void aCustomerSeesTheShelvesInTheOrderHomaSet() {
            String third = shelfIsCreated("Laundry day", 1, "LAUNDRY_FOLD");
            String first = shelfIsCreated("Weekend quick refresh", 1, "KITCHEN", "LAUNDRY_FOLD");
            String second = shelfIsCreated("Kitchen rescue", 2, "KITCHEN");

            assertThat(customersView.view().shelves()).extracting(CustomerViewShelvesResponse.Shelf::shelfId)
                    .containsExactly(first, second, third);
        }

        @Test
        void aCustomerSeesEachShelfsTitleAndTheSearchItRuns() {
            String shelfId = shelfIsCreated("Weekend quick refresh", 1, "KITCHEN", "LAUNDRY_FOLD");

            CustomerViewShelvesResponse.Shelf shelf = customersView.view().shelves().get(0);

            assertThat(shelf).isEqualTo(new CustomerViewShelvesResponse.Shelf(
                    shelfId, "Weekend quick refresh", Set.of("KITCHEN", "LAUNDRY_FOLD"), 2, 1));
            assertThat(shelf.shelfId()).isEqualTo(shelfId);
            assertThat(shelf.title()).isEqualTo("Weekend quick refresh");
            assertThat(shelf.jobCodes()).containsExactlyInAnyOrder("KITCHEN", "LAUNDRY_FOLD");
            assertThat(shelf.hours()).isEqualTo(2);
            assertThat(shelf.helpers()).isEqualTo(1);
        }
    }
}
