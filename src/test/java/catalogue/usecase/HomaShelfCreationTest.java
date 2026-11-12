package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfJobNotOfferedException;
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryHomepages;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryJobLists;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShelves;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.exception.ChangedMeanwhileException;
import catalogue.usecase.exception.DecidedByRequiredException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Homa's founders create the homepage shelves. */
class HomaShelfCreationTest {

    private final InMemoryShelves shelves = new InMemoryShelves();
    private final AuditLog auditLog = new AuditLog();
    private final InMemoryJobLists jobLists = new InMemoryJobLists();
    private final InMemoryHomepages homepages = new InMemoryHomepages();
    private final HomaCreateShelf createShelf = new HomaCreateShelf(shelves, homepages, jobLists, auditLog, CLOCK);

    HomaShelfCreationTest() {
        HomaAddJob addJob = new HomaAddJob(jobLists, new PublishedEvents(), new AuditLog(), CLOCK);
        addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen"));
        addJob.add(new AddJobRequest(FOUNDER, "LAUNDRY_FOLD", "Laundry folding", "Laundry"));
        addJob.add(new AddJobRequest(FOUNDER, "GARDEN", "Garden tidy", "Outdoors"));
        new HomaRetireJob(jobLists, shelves, new PublishedEvents(), new AuditLog(), CLOCK)
                .retire(new RetireJobRequest(FOUNDER, "GARDEN", "No provider offers it"));
    }

    private static CreateShelfRequest weekendQuickRefresh() {
        return new CreateShelfRequest(FOUNDER, " Weekend quick refresh ", Set.of("KITCHEN", "LAUNDRY_FOLD"), 2, 1, 1);
    }

    @Test
    void aCreatedShelfIsSavedWithItsPresetSearchAndPlace() {
        CreateShelfResponse created = createShelf.create(weekendQuickRefresh());

        assertThat(shelves.findAll()).hasSize(1);
        Shelf saved = shelves.findAll().get(0);
        assertThat(saved.title()).isEqualTo("Weekend quick refresh");
        assertThat(saved.preset().jobs()).containsExactlyInAnyOrder(JobCode.of("KITCHEN"), JobCode.of("LAUNDRY_FOLD"));
        assertThat(saved.preset().hours()).isEqualTo(2);
        assertThat(saved.preset().helpers()).isEqualTo(1);
        assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(saved.id());
        assertThat(created).isEqualTo(new CreateShelfResponse(saved.id(), "Weekend quick refresh", 1));
        assertThat(created.shelfId()).isEqualTo(saved.id());
        assertThat(created.title()).isEqualTo("Weekend quick refresh");
        assertThat(created.order()).isEqualTo(1);
    }

    @Test
    void aCreatedShelfIsRecordedWithWhoCreatedItAndWhen() {
        CreateShelfResponse created = createShelf.create(weekendQuickRefresh());

        assertThat(auditLog.all).containsExactly(recorded("CREATE_SHELF", created.shelfId()));
    }

    @Test
    void aShelfWithNoJobToSearchForIsRefusedAndNothingIsSavedOrRecorded() {
        CreateShelfRequest noJobs = new CreateShelfRequest(FOUNDER, "Weekend quick refresh", Set.of(), 2, 1, 1);

        assertThatThrownBy(() -> createShelf.create(noJobs)).isInstanceOf(EmptyPresetSearchException.class);

        assertThat(shelves.findAll()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void aShelfCannotSearchForAJobThatIsNotOnTheList() {
        CreateShelfRequest unknownJob = new CreateShelfRequest(FOUNDER, "Pool day", Set.of("KITCHEN", "POOL"), 2, 1, 1);

        assertThatThrownBy(() -> createShelf.create(unknownJob))
                .isInstanceOf(ShelfJobNotOfferedException.class)
                .hasMessageContaining("POOL");

        assertThat(shelves.findAll()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void aShelfCannotSearchForARetiredJob() {
        CreateShelfRequest retiredJob = new CreateShelfRequest(FOUNDER, "Garden day", Set.of("GARDEN"), 2, 1, 1);

        assertThatThrownBy(() -> createShelf.create(retiredJob)).isInstanceOf(ShelfJobNotOfferedException.class);

        assertThat(shelves.findAll()).isEmpty();
    }

    @Test
    void beforeAnyJobIsAddedNoShelfCanBeCreated() {
        HomaCreateShelf withNoJobList = new HomaCreateShelf(shelves, homepages, new InMemoryJobLists(), auditLog, CLOCK);

        assertThatThrownBy(() -> withNoJobList.create(weekendQuickRefresh())).isInstanceOf(ShelfJobNotOfferedException.class);
    }

    @Test
    void aShelfMustHaveAPlaceOnTheHomepage() {
        CreateShelfRequest noPlace = new CreateShelfRequest(FOUNDER, "Weekend quick refresh", Set.of("KITCHEN"), 2, 1, 0);

        assertThatThrownBy(() -> createShelf.create(noPlace)).isInstanceOf(InvalidShelfOrderException.class);

        assertThat(shelves.findAll()).isEmpty();
        assertThat(homepages.find()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void aShelfIsPlacedNoFurtherThanOnePastTheLast() {
        createShelf.create(weekendQuickRefresh());
        CreateShelfRequest tooFar = new CreateShelfRequest(FOUNDER, "Kitchen rescue", Set.of("KITCHEN"), 2, 1, 3);

        assertThatThrownBy(() -> createShelf.create(tooFar))
                .isInstanceOf(InvalidShelfOrderException.class)
                .hasMessage("A shelf's place is from 1 to 2, was 3");

        assertThat(shelves.findAll()).hasSize(1);
        assertThat(homepages.find().orElseThrow().shelfIds()).hasSize(1);
    }

    @Test
    void aShelfPlacedFirstMovesTheOthersDown() {
        CreateShelfResponse weekend = createShelf.create(weekendQuickRefresh());

        CreateShelfResponse kitchen = createShelf.create(
                new CreateShelfRequest(FOUNDER, "Kitchen rescue", Set.of("KITCHEN"), 2, 1, 1));

        assertThat(kitchen.order()).isEqualTo(1);
        assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(kitchen.shelfId(), weekend.shelfId());
        assertThat(homepages.saves).isEqualTo(2);
    }

    @Test
    void aShelfIsNotCreatedWhenAnotherFounderChangedTheHomepageFirst() {
        homepages.anotherFounderSavesFirst();

        assertThatThrownBy(() -> createShelf.create(weekendQuickRefresh())).isInstanceOf(ChangedMeanwhileException.class);

        assertThat(homepages.find()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void creatingAShelfNeedsTheFounderWhoDecidedIt() {
        CreateShelfRequest byNoOne = new CreateShelfRequest("", "Weekend quick refresh", Set.of("KITCHEN"), 2, 1, 1);

        assertThatThrownBy(() -> createShelf.create(byNoOne)).isInstanceOf(DecidedByRequiredException.class);

        assertThat(shelves.findAll()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }
}
