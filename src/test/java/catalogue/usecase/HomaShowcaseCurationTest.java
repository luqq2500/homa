package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.JobCode;
import catalogue.domain.Showcase;
import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.domain.exception.FeaturedSearchJobNotOfferedException;
import catalogue.domain.exception.FeaturedSearchNotInShowcaseException;
import catalogue.domain.exception.FeaturedSearchTitleRequiredException;
import catalogue.domain.exception.InvalidShowcasePositionException;
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryJobLists;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryShowcases;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddJobRequest;
import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;
import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;
import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;
import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.exception.ChangedMeanwhileException;
import catalogue.usecase.exception.DecidedByRequiredException;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Homa's founders feature searches on the homepage, and rename, move and remove them. */
class HomaShowcaseCurationTest {

    private final InMemoryJobLists jobLists = new InMemoryJobLists();
    private final InMemoryShowcases showcases = new InMemoryShowcases();
    private final AuditLog auditLog = new AuditLog();
    private final HomaFeatureSearch feature = new HomaFeatureSearch(showcases, jobLists, auditLog, CLOCK);
    private final HomaRenameFeaturedSearch rename = new HomaRenameFeaturedSearch(showcases, auditLog, CLOCK);
    private final HomaMoveFeaturedSearch move = new HomaMoveFeaturedSearch(showcases, auditLog, CLOCK);
    private final HomaRemoveFeaturedSearch remove = new HomaRemoveFeaturedSearch(showcases, auditLog, CLOCK);

    HomaShowcaseCurationTest() {
        HomaAddJob addJob = new HomaAddJob(jobLists, new PublishedEvents(), new AuditLog(), CLOCK);
        addJob.add(new AddJobRequest(FOUNDER, "KITCHEN", "Kitchen care", "Kitchen"));
        addJob.add(new AddJobRequest(FOUNDER, "LAUNDRY_FOLD", "Laundry folding", "Laundry"));
        addJob.add(new AddJobRequest(FOUNDER, "GARDEN", "Garden tidy", "Outdoors"));
        new HomaRetireJob(jobLists, showcases, new PublishedEvents(), new AuditLog(), CLOCK)
                .retire(new RetireJobRequest(FOUNDER, "GARDEN", "No provider offers it"));
    }

    private FeatureSearchResponse featuring(String title, int position, String... jobs) {
        return feature.feature(new FeatureSearchRequest(FOUNDER, title, Set.of(jobs), 2, 1, position));
    }

    private Showcase saved() {
        return showcases.find().orElseThrow();
    }

    private java.util.List<String> idsInOrder() {
        return saved().featuredSearches().stream().map(FeaturedSearch::id).toList();
    }

    @Nested
    class Featuring {

        @Test
        void aFeaturedSearchIsSavedWithItsPresetSearchAndPlace() {
            FeatureSearchResponse featured = feature.feature(
                    new FeatureSearchRequest(FOUNDER, " Weekend quick refresh ", Set.of("KITCHEN", "LAUNDRY_FOLD"), 3, 2, 1));

            FeaturedSearch saved = saved().featuredSearches().get(0);
            assertThat(saved.title()).isEqualTo("Weekend quick refresh");
            assertThat(saved.preset().jobs()).containsExactlyInAnyOrder(JobCode.of("KITCHEN"), JobCode.of("LAUNDRY_FOLD"));
            assertThat(saved.preset().hours()).isEqualTo(3);
            assertThat(saved.preset().helpers()).isEqualTo(2);
            assertThat(featured).isEqualTo(new FeatureSearchResponse(saved.id(), "Weekend quick refresh", 1));
            assertThat(featured.featuredSearchId()).isEqualTo(saved.id());
            assertThat(featured.title()).isEqualTo("Weekend quick refresh");
            assertThat(featured.position()).isEqualTo(1);
            assertThat(showcases.saves).isEqualTo(1);
        }

        @Test
        void aFeaturedSearchIsRecordedWithWhoFeaturedItAndWhen() {
            FeatureSearchResponse featured = featuring("Weekend quick refresh", 1, "KITCHEN");

            assertThat(auditLog.all).containsExactly(recorded("FEATURE_SEARCH", featured.featuredSearchId()));
        }

        @Test
        void aSearchFeaturedFirstMovesTheOthersDown() {
            FeatureSearchResponse weekend = featuring("Weekend quick refresh", 1, "KITCHEN");

            FeatureSearchResponse kitchen = featuring("Kitchen rescue", 1, "KITCHEN");

            assertThat(kitchen.position()).isEqualTo(1);
            assertThat(idsInOrder()).containsExactly(kitchen.featuredSearchId(), weekend.featuredSearchId());
            assertThat(showcases.saves).isEqualTo(2);
        }

        @Test
        void aSearchForAJobThatIsNotOnTheListIsNotFeatured() {
            assertThatThrownBy(() -> featuring("Pool day", 1, "KITCHEN", "POOL"))
                    .isInstanceOf(FeaturedSearchJobNotOfferedException.class)
                    .hasMessageContaining("POOL");
            assertThat(showcases.find()).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aSearchForARetiredJobIsNotFeatured() {
            assertThatThrownBy(() -> featuring("Garden day", 1, "GARDEN")).isInstanceOf(FeaturedSearchJobNotOfferedException.class);
            assertThat(showcases.find()).isEmpty();
        }

        @Test
        void beforeAnyJobIsAddedNothingCanBeFeatured() {
            HomaFeatureSearch withNoJobList = new HomaFeatureSearch(showcases, new InMemoryJobLists(), auditLog, CLOCK);

            assertThatThrownBy(() -> withNoJobList.feature(new FeatureSearchRequest(FOUNDER, "A", Set.of("KITCHEN"), 2, 1, 1)))
                    .isInstanceOf(FeaturedSearchJobNotOfferedException.class);
        }

        @Test
        void aSearchWithNoJobIsNotFeatured() {
            assertThatThrownBy(() -> featuring("Nothing", 1)).isInstanceOf(EmptyPresetSearchException.class);
            assertThat(showcases.find()).isEmpty();
        }

        @Test
        void aSearchIsFeaturedNoFurtherThanOnePastTheLast() {
            featuring("Weekend quick refresh", 1, "KITCHEN");

            assertThatThrownBy(() -> featuring("Kitchen rescue", 3, "KITCHEN"))
                    .isInstanceOf(InvalidShowcasePositionException.class)
                    .hasMessage("A place in the showcase is from 1 to 2, was 3");
            assertThat(showcases.saves).isEqualTo(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void featuringASearchNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> feature.feature(new FeatureSearchRequest("", "A", Set.of("KITCHEN"), 2, 1, 1)))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(showcases.find()).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aSearchIsNotFeaturedWhenAnotherFounderChangedTheShowcaseFirst() {
            showcases.anotherFounderSavesFirst();

            assertThatThrownBy(() -> featuring("Weekend quick refresh", 1, "KITCHEN")).isInstanceOf(ChangedMeanwhileException.class);
            assertThat(showcases.find()).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }
    }

    @Nested
    class AfterThreeAreFeatured {

        private final String weekend;
        private final String kitchen;
        private final String laundry;

        AfterThreeAreFeatured() {
            weekend = featuring("Weekend quick refresh", 1, "KITCHEN", "LAUNDRY_FOLD").featuredSearchId();
            kitchen = featuring("Kitchen rescue", 2, "KITCHEN").featuredSearchId();
            laundry = featuring("Laundry day", 3, "LAUNDRY_FOLD").featuredSearchId();
            auditLog.all.clear();
            showcases.saves = 0;
        }

        private String title(String id) {
            return saved().featuredSearch(id).orElseThrow().title();
        }

        @Test
        void aRenamedSearchIsSavedAndRecorded() {
            RenameFeaturedSearchResponse renamed = rename.rename(new RenameFeaturedSearchRequest(FOUNDER, kitchen, " Kitchen SOS "));

            assertThat(renamed).isEqualTo(new RenameFeaturedSearchResponse(kitchen, "Kitchen SOS"));
            assertThat(renamed.featuredSearchId()).isEqualTo(kitchen);
            assertThat(renamed.title()).isEqualTo("Kitchen SOS");
            assertThat(title(kitchen)).isEqualTo("Kitchen SOS");
            assertThat(showcases.saves).isEqualTo(1);
            assertThat(auditLog.all).containsExactly(recorded("RENAME_FEATURED_SEARCH", kitchen));
        }

        @Test
        void renamingToItsOwnTitleSavesAndRecordsNothing() {
            RenameFeaturedSearchResponse renamed = rename.rename(new RenameFeaturedSearchRequest(FOUNDER, kitchen, "Kitchen rescue"));

            assertThat(renamed.title()).isEqualTo("Kitchen rescue");
            assertThat(showcases.saves).isZero();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aRenamedSearchStillNeedsATitle() {
            assertThatThrownBy(() -> rename.rename(new RenameFeaturedSearchRequest(FOUNDER, kitchen, " ")))
                    .isInstanceOf(FeaturedSearchTitleRequiredException.class);
            assertThat(showcases.saves).isZero();
        }

        @Test
        void aSearchThatIsNotFeaturedIsNotRenamed() {
            assertThatThrownBy(() -> rename.rename(new RenameFeaturedSearchRequest(FOUNDER, "no-such-search", "Anything")))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class);
        }

        @Test
        void renamingNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> rename.rename(new RenameFeaturedSearchRequest(null, kitchen, "Kitchen SOS")))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(title(kitchen)).isEqualTo("Kitchen rescue");
        }

        @Test
        void aMovedSearchIsSavedInItsNewPlaceAndRecorded() {
            MoveFeaturedSearchResponse moved = move.move(new MoveFeaturedSearchRequest(FOUNDER, laundry, 1));

            assertThat(moved.featuredSearchIds()).containsExactly(laundry, weekend, kitchen);
            assertThat(idsInOrder()).containsExactly(laundry, weekend, kitchen);
            assertThat(showcases.saves).isEqualTo(1);
            assertThat(auditLog.all).containsExactly(recorded("MOVE_FEATURED_SEARCH", laundry));
        }

        @Test
        void movingToItsOwnPlaceSavesAndRecordsNothing() {
            MoveFeaturedSearchResponse moved = move.move(new MoveFeaturedSearchRequest(FOUNDER, kitchen, 2));

            assertThat(moved.featuredSearchIds()).containsExactly(weekend, kitchen, laundry);
            assertThat(showcases.saves).isZero();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aSearchMovesOnlyWithinTheShowcase() {
            assertThatThrownBy(() -> move.move(new MoveFeaturedSearchRequest(FOUNDER, kitchen, 4)))
                    .isInstanceOf(InvalidShowcasePositionException.class);
            assertThat(idsInOrder()).containsExactly(weekend, kitchen, laundry);
        }

        @Test
        void movingNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> move.move(new MoveFeaturedSearchRequest(" ", laundry, 1)))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(idsInOrder()).containsExactly(weekend, kitchen, laundry);
        }

        @Test
        void aMoveIsRefusedWhenAnotherFounderChangedTheShowcaseFirst() {
            showcases.anotherFounderSavesFirst();

            assertThatThrownBy(() -> move.move(new MoveFeaturedSearchRequest(FOUNDER, laundry, 1)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void aRemovedSearchIsSavedRecordedAndTheOthersMoveUp() {
            RemoveFeaturedSearchResponse removed = remove.remove(new RemoveFeaturedSearchRequest(FOUNDER, weekend));

            assertThat(removed.featuredSearchIds()).containsExactly(kitchen, laundry);
            assertThat(idsInOrder()).containsExactly(kitchen, laundry);
            assertThat(saved().featuredSearch(weekend)).isEmpty();
            assertThat(showcases.saves).isEqualTo(1);
            assertThat(auditLog.all).containsExactly(recorded("REMOVE_FEATURED_SEARCH", weekend));
        }

        @Test
        void aSearchThatIsNotFeaturedIsNotRemoved() {
            assertThatThrownBy(() -> remove.remove(new RemoveFeaturedSearchRequest(FOUNDER, "no-such-search")))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class);
            assertThat(idsInOrder()).hasSize(3);
            assertThat(showcases.saves).isZero();
        }

        @Test
        void removingNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> remove.remove(new RemoveFeaturedSearchRequest(" ", weekend)))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(idsInOrder()).hasSize(3);
        }

        @Test
        void aRemovalIsRefusedWhenAnotherFounderChangedTheShowcaseFirst() {
            showcases.anotherFounderSavesFirst();

            assertThatThrownBy(() -> remove.remove(new RemoveFeaturedSearchRequest(FOUNDER, weekend)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(auditLog.all).isEmpty();
        }
    }

    @Nested
    class BeforeAnythingIsFeatured {

        @Test
        void nothingIsRenamedMovedOrRemoved() {
            assertThatThrownBy(() -> rename.rename(new RenameFeaturedSearchRequest(FOUNDER, "a", "A")))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class);
            assertThatThrownBy(() -> move.move(new MoveFeaturedSearchRequest(FOUNDER, "a", 1)))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class);
            assertThatThrownBy(() -> remove.remove(new RemoveFeaturedSearchRequest(FOUNDER, "a")))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class);
            assertThat(showcases.find()).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }
    }
}
