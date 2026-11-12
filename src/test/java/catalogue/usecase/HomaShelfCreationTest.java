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
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShelves;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;
import catalogue.usecase.exception.DecidedByRequiredException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Homa's founders create the homepage shelves. */
class HomaShelfCreationTest {

    private final InMemoryShelves shelves = new InMemoryShelves();
    private final AuditLog auditLog = new AuditLog();
    private final HomaCreateShelf createShelf = new HomaCreateShelf(shelves, auditLog, CLOCK);

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
        assertThat(saved.order()).isEqualTo(1);
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
    void aShelfMustHaveAPlaceOnTheHomepage() {
        CreateShelfRequest noPlace = new CreateShelfRequest(FOUNDER, "Weekend quick refresh", Set.of("KITCHEN"), 2, 1, 0);

        assertThatThrownBy(() -> createShelf.create(noPlace)).isInstanceOf(InvalidShelfOrderException.class);

        assertThat(shelves.findAll()).isEmpty();
    }

    @Test
    void creatingAShelfNeedsTheFounderWhoDecidedIt() {
        CreateShelfRequest byNoOne = new CreateShelfRequest("", "Weekend quick refresh", Set.of("KITCHEN"), 2, 1, 1);

        assertThatThrownBy(() -> createShelf.create(byNoOne)).isInstanceOf(DecidedByRequiredException.class);

        assertThat(shelves.findAll()).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }
}
