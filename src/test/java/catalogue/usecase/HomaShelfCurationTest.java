package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfNotOnHomepageException;
import catalogue.domain.exception.ShelfTitleRequiredException;
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryHomepages;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryJobLists;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShelves;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.MoveShelfRequest;
import catalogue.usecase.dto.MoveShelfResponse;
import catalogue.usecase.dto.RemoveShelfRequest;
import catalogue.usecase.dto.RemoveShelfResponse;
import catalogue.usecase.dto.RenameShelfRequest;
import catalogue.usecase.dto.RenameShelfResponse;
import catalogue.usecase.exception.ChangedMeanwhileException;
import catalogue.usecase.exception.DecidedByRequiredException;
import catalogue.usecase.exception.ShelfNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Homa's founders rename, move and remove the homepage shelves. */
class HomaShelfCurationTest {

    private final InMemoryShelves shelves = new InMemoryShelves();
    private final InMemoryHomepages homepages = new InMemoryHomepages();
    private final AuditLog auditLog = new AuditLog();
    private final HomaRenameShelf renameShelf = new HomaRenameShelf(shelves, auditLog, CLOCK);
    private final HomaMoveShelf moveShelf = new HomaMoveShelf(homepages, auditLog, CLOCK);
    private final HomaRemoveShelf removeShelf = new HomaRemoveShelf(shelves, homepages, auditLog, CLOCK);
    private final String weekend;
    private final String kitchen;
    private final String laundry;

    HomaShelfCurationTest() {
        InMemoryJobLists jobLists = new InMemoryJobLists();
        HomaAddJob addJob = new HomaAddJob(jobLists, new PublishedEvents(), new AuditLog(), CLOCK);
        addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen"));
        addJob.add(new AddJobRequest(FOUNDER, "LAUNDRY_FOLD", "Laundry folding", "Laundry"));
        HomaCreateShelf create = new HomaCreateShelf(shelves, homepages, jobLists, new AuditLog(), CLOCK);
        weekend = create.create(new CreateShelfRequest(FOUNDER, "Weekend quick refresh", Set.of("KITCHEN", "LAUNDRY_FOLD"), 2, 1, 1)).shelfId();
        kitchen = create.create(new CreateShelfRequest(FOUNDER, "Kitchen rescue", Set.of("KITCHEN"), 2, 1, 2)).shelfId();
        laundry = create.create(new CreateShelfRequest(FOUNDER, "Laundry day", Set.of("LAUNDRY_FOLD"), 2, 1, 3)).shelfId();
    }

    private String title(String shelfId) {
        return shelves.find(shelfId).orElseThrow().title();
    }

    private int shelfSaves() {
        return shelves.saves;
    }

    @Nested
    class Renaming {

        @Test
        void aRenamedShelfIsSavedAndRecorded() {
            int before = shelfSaves();

            RenameShelfResponse renamed = renameShelf.rename(new RenameShelfRequest(FOUNDER, kitchen, " Kitchen SOS "));

            assertThat(renamed).isEqualTo(new RenameShelfResponse(kitchen, "Kitchen SOS"));
            assertThat(renamed.shelfId()).isEqualTo(kitchen);
            assertThat(renamed.title()).isEqualTo("Kitchen SOS");
            assertThat(title(kitchen)).isEqualTo("Kitchen SOS");
            assertThat(shelfSaves()).isEqualTo(before + 1);
            assertThat(auditLog.all).containsExactly(recorded("RENAME_SHELF", kitchen));
        }

        @Test
        void renamingAShelfToItsOwnTitleSavesAndRecordsNothing() {
            int before = shelfSaves();

            RenameShelfResponse renamed = renameShelf.rename(new RenameShelfRequest(FOUNDER, kitchen, "Kitchen rescue"));

            assertThat(renamed.title()).isEqualTo("Kitchen rescue");
            assertThat(shelfSaves()).isEqualTo(before);
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aRenamedShelfStillNeedsATitle() {
            assertThatThrownBy(() -> renameShelf.rename(new RenameShelfRequest(FOUNDER, kitchen, " ")))
                    .isInstanceOf(ShelfTitleRequiredException.class);
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aShelfThatDoesNotExistIsNotRenamed() {
            assertThatThrownBy(() -> renameShelf.rename(new RenameShelfRequest(FOUNDER, "no-such-shelf", "Anything")))
                    .isInstanceOf(ShelfNotFoundException.class)
                    .hasMessage("There is no shelf no-such-shelf");
        }

        @Test
        void renamingAShelfNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> renameShelf.rename(new RenameShelfRequest(null, kitchen, "Kitchen SOS")))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(title(kitchen)).isEqualTo("Kitchen rescue");
        }

        @Test
        void aRenameIsRefusedWhenAnotherFounderSavedTheShelfFirst() {
            shelves.anotherFounderSavesFirst();

            assertThatThrownBy(() -> renameShelf.rename(new RenameShelfRequest(FOUNDER, kitchen, "Kitchen SOS")))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(auditLog.all).isEmpty();
        }
    }

    @Nested
    class Moving {

        @Test
        void aMovedShelfIsSavedInItsNewPlaceAndRecorded() {
            int before = homepages.saves;

            MoveShelfResponse moved = moveShelf.move(new MoveShelfRequest(FOUNDER, laundry, 1));

            assertThat(moved.shelfIds()).containsExactly(laundry, weekend, kitchen);
            assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(laundry, weekend, kitchen);
            assertThat(homepages.saves).isEqualTo(before + 1);
            assertThat(auditLog.all).containsExactly(recorded("MOVE_SHELF", laundry));
        }

        @Test
        void movingAShelfToItsOwnPlaceSavesAndRecordsNothing() {
            int before = homepages.saves;

            MoveShelfResponse moved = moveShelf.move(new MoveShelfRequest(FOUNDER, kitchen, 2));

            assertThat(moved.shelfIds()).containsExactly(weekend, kitchen, laundry);
            assertThat(homepages.saves).isEqualTo(before);
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aShelfMovesOnlyWithinTheHomepage() {
            assertThatThrownBy(() -> moveShelf.move(new MoveShelfRequest(FOUNDER, kitchen, 4)))
                    .isInstanceOf(InvalidShelfOrderException.class);
            assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(weekend, kitchen, laundry);
        }

        @Test
        void aShelfNotOnTheHomepageDoesNotMove() {
            assertThatThrownBy(() -> moveShelf.move(new MoveShelfRequest(FOUNDER, "no-such-shelf", 1)))
                    .isInstanceOf(ShelfNotOnHomepageException.class);
        }

        @Test
        void noShelfMovesBeforeAnyIsCreated() {
            HomaMoveShelf onAnEmptyHomepage = new HomaMoveShelf(new InMemoryHomepages(), auditLog, CLOCK);

            assertThatThrownBy(() -> onAnEmptyHomepage.move(new MoveShelfRequest(FOUNDER, kitchen, 1)))
                    .isInstanceOf(ShelfNotFoundException.class);
        }

        @Test
        void movingAShelfNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> moveShelf.move(new MoveShelfRequest("", laundry, 1)))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(weekend, kitchen, laundry);
        }

        @Test
        void aMoveIsRefusedWhenAnotherFounderChangedTheHomepageFirst() {
            homepages.anotherFounderSavesFirst();

            assertThatThrownBy(() -> moveShelf.move(new MoveShelfRequest(FOUNDER, laundry, 1)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(auditLog.all).isEmpty();
        }
    }

    @Nested
    class Removing {

        @Test
        void aRemovedShelfIsDeletedAndTheOthersMoveUp() {
            RemoveShelfResponse removed = removeShelf.remove(new RemoveShelfRequest(FOUNDER, weekend));

            assertThat(removed.shelfIds()).containsExactly(kitchen, laundry);
            assertThat(homepages.find().orElseThrow().shelfIds()).containsExactly(kitchen, laundry);
            assertThat(shelves.find(weekend)).isEmpty();
            assertThat(shelves.findAll()).hasSize(2);
            assertThat(auditLog.all).containsExactly(recorded("REMOVE_SHELF", weekend));
        }

        @Test
        void aShelfThatDoesNotExistIsNotRemoved() {
            assertThatThrownBy(() -> removeShelf.remove(new RemoveShelfRequest(FOUNDER, "no-such-shelf")))
                    .isInstanceOf(ShelfNotFoundException.class);
            assertThat(shelves.findAll()).hasSize(3);
        }

        @Test
        void removingAShelfNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> removeShelf.remove(new RemoveShelfRequest(" ", weekend)))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(shelves.find(weekend)).isPresent();
        }

        @Test
        void aRemovalIsRefusedWhenAnotherFounderChangedTheHomepageFirstAndTheShelfIsKept() {
            homepages.anotherFounderSavesFirst();

            assertThatThrownBy(() -> removeShelf.remove(new RemoveShelfRequest(FOUNDER, weekend)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(shelves.find(weekend)).isPresent();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aShelfIsNotRemovedFromAHomepageThatDoesNotExist() {
            HomaRemoveShelf withNoHomepage = new HomaRemoveShelf(shelves, new InMemoryHomepages(), auditLog, CLOCK);

            assertThatThrownBy(() -> withNoHomepage.remove(new RemoveShelfRequest(FOUNDER, weekend)))
                    .isInstanceOf(ShelfNotFoundException.class);
            assertThat(shelves.find(weekend)).isPresent();
        }
    }
}
