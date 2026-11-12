package catalogue.domain;

import static catalogue.domain.CatalogueBuilders.aPresetSearchFor;
import static catalogue.domain.CatalogueBuilders.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.domain.exception.FeaturedSearchJobNotOfferedException;
import catalogue.domain.exception.FeaturedSearchNotInShowcaseException;
import catalogue.domain.exception.FeaturedSearchTitleRequiredException;
import catalogue.domain.exception.InvalidShowcasePositionException;
import catalogue.domain.exception.PresetSearchOutOfRangeException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: The homepage showcase. Homa features searches, in an order with no gaps and no two in one place. */
class ShowcaseCurationTest {

    private static final Set<JobCode> OFFERED = Set.of(job("KITCHEN"), job("LIVING_ROOM_TIDY"), job("LAUNDRY_FOLD"));

    private Showcase showcase = Showcase.create();

    private String featured(String title, int position, String... jobs) {
        return showcase.feature(title, aPresetSearchFor(jobs), position, OFFERED).id();
    }

    private List<String> titlesInOrder() {
        return showcase.featuredSearches().stream().map(FeaturedSearch::title).toList();
    }

    /** A showcase with "A", "B" and "C" featured, in that order. */
    private void aBAndCAreFeatured() {
        featured("A", 1, "KITCHEN");
        featured("B", 2, "KITCHEN");
        featured("C", 3, "KITCHEN");
    }

    private String idOf(String title) {
        return showcase.featuredSearches().stream().filter(search -> search.title().equals(title)).findFirst().orElseThrow().id();
    }

    @Test
    void aNewShowcaseFeaturesNothing() {
        assertThat(showcase.featuredSearches()).isEmpty();
        assertThat(showcase.id()).isNotBlank().isNotEqualTo(Showcase.create().id());
    }

    @Nested
    class PresetSearches {

        @Test
        void aFeaturedSearchNeedsAPresetSearchWithAtLeastOneJob() {
            assertThatThrownBy(() -> PresetSearch.of(Set.of(), 2, 1)).isInstanceOf(EmptyPresetSearchException.class);
        }

        @Test
        void aPresetSearchNeedsAtLeastOneHour() {
            assertThat(PresetSearch.of(Set.of(job("KITCHEN")), 1, 1).hours()).isEqualTo(1);
            assertThatThrownBy(() -> PresetSearch.of(Set.of(job("KITCHEN")), 0, 1))
                    .isInstanceOf(PresetSearchOutOfRangeException.class)
                    .hasMessage("A preset search needs at least one hour, was 0");
        }

        @Test
        void aPresetSearchNeedsAtLeastOneHelper() {
            assertThat(PresetSearch.of(Set.of(job("KITCHEN")), 1, 1).helpers()).isEqualTo(1);
            assertThatThrownBy(() -> PresetSearch.of(Set.of(job("KITCHEN")), 1, 0))
                    .isInstanceOf(PresetSearchOutOfRangeException.class)
                    .hasMessage("A preset search needs at least one helper, was 0");
        }

        @Test
        void aPresetSearchKeepsItsJobsEvenIfTheOriginalSetChanges() {
            Set<JobCode> jobs = new HashSet<>(Set.of(job("KITCHEN"), job("LAUNDRY_FOLD")));
            PresetSearch preset = PresetSearch.of(jobs, 3, 2);

            jobs.clear();

            assertThat(preset.jobs()).containsExactlyInAnyOrder(job("KITCHEN"), job("LAUNDRY_FOLD"));
        }
    }

    @Nested
    class Featuring {

        @Test
        void aFeaturedSearchHasItsTitleAndPresetSearchAndIsShown() {
            PresetSearch preset = aPresetSearchFor("KITCHEN", "LIVING_ROOM_TIDY");

            FeaturedSearch search = showcase.feature(" Weekend quick refresh ", preset, 1, OFFERED);

            assertThat(search.title()).isEqualTo("Weekend quick refresh");
            assertThat(search.preset()).isEqualTo(preset);
            assertThat(search.isShown()).isTrue();
            assertThat(search.id()).isNotBlank();
            assertThat(showcase.featuredSearch(search.id())).containsSame(search);
        }

        @Test
        void eachFeaturedSearchHasItsOwnId() {
            assertThat(featured("A", 1, "KITCHEN")).isNotEqualTo(featured("B", 2, "KITCHEN"));
        }

        @ParameterizedTest(name = "\"{0}\" is not a title")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aFeaturedSearchNeedsATitle(String title) {
            assertThatThrownBy(() -> showcase.feature(title, aPresetSearchFor("KITCHEN"), 1, OFFERED))
                    .isInstanceOf(FeaturedSearchTitleRequiredException.class)
                    .hasMessage("A featured search needs a title");
            assertThat(showcase.featuredSearches()).isEmpty();
        }

        @Test
        void aFeaturedSearchNeedsAPresetSearch() {
            assertThatThrownBy(() -> showcase.feature("A", null, 1, OFFERED)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void aFeaturedSearchSearchesOnlyForJobsProvidersCanOffer() {
            assertThatThrownBy(() -> showcase.feature("Garden day", aPresetSearchFor("KITCHEN", "GARDEN"), 1, OFFERED))
                    .isInstanceOf(FeaturedSearchJobNotOfferedException.class)
                    .hasMessage("A featured search cannot search for GARDEN: it is not a job providers can offer");
            assertThat(showcase.featuredSearches()).isEmpty();
        }

        @Test
        void theFirstJobNotOfferedIsNamedInAlphabeticalOrder() {
            assertThatThrownBy(() -> showcase.feature("Outdoors", aPresetSearchFor("POOL", "GARDEN", "KITCHEN"), 1, OFFERED))
                    .hasMessageContaining("GARDEN");
        }

        @Test
        void aSearchFeaturedInTheMiddleMovesTheRestDownOne() {
            aBAndCAreFeatured();

            String added = featured("new", 2, "KITCHEN");

            assertThat(titlesInOrder()).containsExactly("A", "new", "B", "C");
            assertThat(showcase.positionOf(added)).isEqualTo(2);
            assertThat(showcase.positionOf(idOf("B"))).isEqualTo(3);
        }

        @Test
        void aSearchFeaturedFirstMovesEveryOtherDown() {
            aBAndCAreFeatured();

            featured("new", 1, "KITCHEN");

            assertThat(titlesInOrder()).containsExactly("new", "A", "B", "C");
        }

        @ParameterizedTest(name = "position {0} is outside 1 to 4")
        @ValueSource(ints = {0, -1, 5})
        void aSearchIsFeaturedFromTheFirstPlaceToOnePastTheLast(int position) {
            aBAndCAreFeatured();

            assertThatThrownBy(() -> featured("new", position, "KITCHEN"))
                    .isInstanceOf(InvalidShowcasePositionException.class)
                    .hasMessage("A place in the showcase is from 1 to 4, was " + position);
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }
    }

    @Nested
    class Renaming {

        @Test
        void aRenamedFeaturedSearchKeepsItsSearchAndPlace() {
            aBAndCAreFeatured();
            String b = idOf("B");

            boolean changed = showcase.rename(b, "  B, renamed ");

            assertThat(changed).isTrue();
            assertThat(titlesInOrder()).containsExactly("A", "B, renamed", "C");
            assertThat(showcase.featuredSearch(b).orElseThrow().preset()).isEqualTo(aPresetSearchFor("KITCHEN"));
        }

        @Test
        void renamingToItsOwnTitleChangesNothing() {
            aBAndCAreFeatured();

            assertThat(showcase.rename(idOf("B"), " B ")).isFalse();
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }

        @Test
        void aRenamedFeaturedSearchStillNeedsATitle() {
            aBAndCAreFeatured();

            assertThatThrownBy(() -> showcase.rename(idOf("B"), " ")).isInstanceOf(FeaturedSearchTitleRequiredException.class);
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }

        @Test
        void onlyAFeaturedSearchInTheShowcaseIsRenamed() {
            assertThatThrownBy(() -> showcase.rename("z", "Anything"))
                    .isInstanceOf(FeaturedSearchNotInShowcaseException.class)
                    .hasMessage("There is no featured search z in the showcase");
        }
    }

    @Nested
    class Moving {

        @Test
        void aSearchMovedUpMovesTheOnesInBetweenDown() {
            aBAndCAreFeatured();

            boolean moved = showcase.move(idOf("C"), 1);

            assertThat(moved).isTrue();
            assertThat(titlesInOrder()).containsExactly("C", "A", "B");
        }

        @Test
        void aSearchMovedDownMovesTheOnesInBetweenUp() {
            aBAndCAreFeatured();

            boolean moved = showcase.move(idOf("A"), 3);

            assertThat(moved).isTrue();
            assertThat(titlesInOrder()).containsExactly("B", "C", "A");
        }

        @Test
        void movingASearchToItsOwnPlaceChangesNothing() {
            aBAndCAreFeatured();

            assertThat(showcase.move(idOf("B"), 2)).isFalse();
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }

        @ParameterizedTest(name = "position {0} is outside 1 to 3")
        @ValueSource(ints = {0, 4})
        void aSearchMovesOnlyBetweenTheFirstAndTheLastPlace(int position) {
            aBAndCAreFeatured();

            assertThatThrownBy(() -> showcase.move(idOf("A"), position))
                    .isInstanceOf(InvalidShowcasePositionException.class)
                    .hasMessage("A place in the showcase is from 1 to 3, was " + position);
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }

        @Test
        void onlyASearchInTheShowcaseMoves() {
            assertThatThrownBy(() -> showcase.move("z", 1)).isInstanceOf(FeaturedSearchNotInShowcaseException.class);
        }
    }

    @Nested
    class Removing {

        @Test
        void aRemovedSearchLeavesNoGap() {
            aBAndCAreFeatured();

            showcase.remove(idOf("A"));

            assertThat(titlesInOrder()).containsExactly("B", "C");
            assertThat(showcase.positionOf(idOf("B"))).isEqualTo(1);
        }

        @Test
        void onlyASearchInTheShowcaseIsRemoved() {
            aBAndCAreFeatured();

            assertThatThrownBy(() -> showcase.remove("z")).isInstanceOf(FeaturedSearchNotInShowcaseException.class);
            assertThat(titlesInOrder()).containsExactly("A", "B", "C");
        }

        @Test
        void onlyASearchInTheShowcaseHasAPlace() {
            assertThatThrownBy(() -> showcase.positionOf("z")).isInstanceOf(FeaturedSearchNotInShowcaseException.class);
        }
    }

    @Nested
    class RetiredJobs {

        @Test
        void aRetiredJobComesOffTheSearchesThatSearchForIt() {
            String weekend = showcase.feature("Weekend", PresetSearch.of(Set.of(job("KITCHEN"), job("LAUNDRY_FOLD")), 3, 2), 1, OFFERED).id();

            boolean changed = showcase.withdrawJob(job("KITCHEN"));

            assertThat(changed).isTrue();
            FeaturedSearch search = showcase.featuredSearch(weekend).orElseThrow();
            assertThat(search.preset()).isEqualTo(PresetSearch.of(Set.of(job("LAUNDRY_FOLD")), 3, 2));
            assertThat(search.isShown()).isTrue();
        }

        @Test
        void aSearchLeftWithNoJobIsNoLongerShownAndKeepsItsPlaceAndLastSearch() {
            featured("Laundry day", 1, "LAUNDRY_FOLD");
            String kitchen = featured("Kitchen rescue", 2, "KITCHEN");

            showcase.withdrawJob(job("KITCHEN"));

            FeaturedSearch search = showcase.featuredSearch(kitchen).orElseThrow();
            assertThat(search.isShown()).isFalse();
            assertThat(search.preset()).isEqualTo(aPresetSearchFor("KITCHEN"));
            assertThat(showcase.positionOf(kitchen)).isEqualTo(2);
        }

        @Test
        void aRetiredJobNoSearchSearchesForChangesNothing() {
            String kitchen = featured("Kitchen rescue", 1, "KITCHEN");

            boolean changed = showcase.withdrawJob(job("LAUNDRY_FOLD"));

            assertThat(changed).isFalse();
            assertThat(showcase.featuredSearch(kitchen).orElseThrow().preset()).isEqualTo(aPresetSearchFor("KITCHEN"));
        }

        @Test
        void aRetiredJobComesOffEverySearchThatHasIt() {
            String first = featured("Kitchen rescue", 1, "KITCHEN");
            String second = featured("Laundry day", 2, "LAUNDRY_FOLD");
            String third = featured("Kitchen again", 3, "KITCHEN");

            showcase.withdrawJob(job("KITCHEN"));

            assertThat(showcase.featuredSearch(first).orElseThrow().isShown()).isFalse();
            assertThat(showcase.featuredSearch(second).orElseThrow().isShown()).isTrue();
            assertThat(showcase.featuredSearch(third).orElseThrow().isShown()).isFalse();
        }
    }

    @Test
    void theOrderShownCannotBeChangedFromOutside() {
        aBAndCAreFeatured();

        assertThatThrownBy(() -> showcase.featuredSearches().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void anUnknownFeaturedSearchIsNotFound() {
        assertThat(showcase.featuredSearch("z")).isEmpty();
    }
}
