package catalogue.domain;

import static catalogue.domain.CatalogueBuilders.aPresetSearchFor;
import static catalogue.domain.CatalogueBuilders.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.PresetSearchOutOfRangeException;
import catalogue.domain.exception.ShelfJobNotOfferedException;
import catalogue.domain.exception.ShelfTitleRequiredException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Shelves. */
class ShelfCurationTest {

    private static final Set<JobCode> OFFERED = Set.of(job("KITCHEN"), job("LIVING_ROOM_TIDY"), job("LAUNDRY_FOLD"));

    @Nested
    class PresetSearches {

        @Test
        void aShelfNeedsAPresetSearchWithAtLeastOneJob() {
            assertThatThrownBy(() -> Shelf.create("Weekend quick refresh", PresetSearch.of(Set.of(), 2, 1), 1, OFFERED))
                    .isInstanceOf(EmptyPresetSearchException.class);
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
    class CreatingShelves {

        @Test
        void aShelfShowsItsTitlePresetSearchAndPlace() {
            PresetSearch preset = aPresetSearchFor("KITCHEN", "LIVING_ROOM_TIDY");

            Shelf shelf = Shelf.create(" Weekend quick refresh ", preset, 1, OFFERED);

            assertThat(shelf.title()).isEqualTo("Weekend quick refresh");
            assertThat(shelf.preset()).isEqualTo(preset);
            assertThat(shelf.order()).isEqualTo(1);
            assertThat(shelf.id()).isNotBlank()
                    .isNotEqualTo(Shelf.create("Another", preset, 2, OFFERED).id());
        }

        @ParameterizedTest(name = "\"{0}\" is not a title")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aShelfNeedsATitle(String title) {
            assertThatThrownBy(() -> Shelf.create(title, aPresetSearchFor("KITCHEN"), 1, OFFERED))
                    .isInstanceOf(ShelfTitleRequiredException.class);
        }

        @Test
        void shelvesAreOrderedFromTheFirstShelf() {
            assertThatThrownBy(() -> Shelf.create("Weekend quick refresh", aPresetSearchFor("KITCHEN"), 0, OFFERED))
                    .isInstanceOf(InvalidShelfOrderException.class)
                    .hasMessageContaining("0");
        }
    }

    @Nested
    class JobsTheShelfSearchesFor {

        @Test
        void aShelfSearchesOnlyForJobsProvidersCanOffer() {
            assertThatThrownBy(() -> Shelf.create("Garden day", aPresetSearchFor("KITCHEN", "GARDEN"), 1, OFFERED))
                    .isInstanceOf(ShelfJobNotOfferedException.class)
                    .hasMessage("A shelf cannot search for GARDEN: it is not a job providers can offer");
        }

        @Test
        void theFirstJobNotOfferedIsNamedInAlphabeticalOrder() {
            assertThatThrownBy(() -> Shelf.create("Outdoors", aPresetSearchFor("POOL", "GARDEN", "KITCHEN"), 1, OFFERED))
                    .hasMessageContaining("GARDEN");
        }

        @Test
        void aNewShelfIsShown() {
            assertThat(Shelf.create("Weekend quick refresh", aPresetSearchFor("KITCHEN"), 1, OFFERED).isShown()).isTrue();
        }

        @Test
        void aRetiredJobComesOffTheShelfsSearch() {
            Shelf shelf = Shelf.create("Weekend quick refresh", PresetSearch.of(Set.of(job("KITCHEN"), job("LAUNDRY_FOLD")), 3, 2), 1, OFFERED);

            boolean changed = shelf.withdrawJob(job("KITCHEN"));

            assertThat(changed).isTrue();
            assertThat(shelf.preset()).isEqualTo(PresetSearch.of(Set.of(job("LAUNDRY_FOLD")), 3, 2));
            assertThat(shelf.isShown()).isTrue();
        }

        @Test
        void aShelfLeftWithNoJobIsNoLongerShown() {
            PresetSearch kitchenOnly = aPresetSearchFor("KITCHEN");
            Shelf shelf = Shelf.create("Kitchen rescue", kitchenOnly, 1, OFFERED);

            boolean changed = shelf.withdrawJob(job("KITCHEN"));

            assertThat(changed).isTrue();
            assertThat(shelf.isShown()).isFalse();
            assertThat(shelf.preset()).isEqualTo(kitchenOnly);
        }

        @Test
        void aRetiredJobTheShelfDoesNotSearchForChangesNothing() {
            PresetSearch kitchenOnly = aPresetSearchFor("KITCHEN");
            Shelf shelf = Shelf.create("Kitchen rescue", kitchenOnly, 1, OFFERED);

            boolean changed = shelf.withdrawJob(job("LAUNDRY_FOLD"));

            assertThat(changed).isFalse();
            assertThat(shelf.preset()).isEqualTo(kitchenOnly);
            assertThat(shelf.isShown()).isTrue();
        }
    }
}
