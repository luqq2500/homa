package com.homa.catalogue.domain;

import static com.homa.catalogue.domain.CatalogueBuilders.aPresetSearchFor;
import static com.homa.catalogue.domain.CatalogueBuilders.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.catalogue.domain.exception.EmptyPresetSearchException;
import com.homa.catalogue.domain.exception.InvalidShelfOrderException;
import com.homa.catalogue.domain.exception.PresetSearchOutOfRangeException;
import com.homa.catalogue.domain.exception.ShelfTitleRequiredException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Shelves. */
class ShelfCurationTest {

    @Nested
    class PresetSearches {

        @Test
        void aShelfNeedsAPresetSearchWithAtLeastOneJob() {
            assertThatThrownBy(() -> Shelf.create("Weekend quick refresh", PresetSearch.of(Set.of(), 2, 1), 1))
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

            Shelf shelf = Shelf.create(" Weekend quick refresh ", preset, 1);

            assertThat(shelf.title()).isEqualTo("Weekend quick refresh");
            assertThat(shelf.preset()).isEqualTo(preset);
            assertThat(shelf.order()).isEqualTo(1);
            assertThat(shelf.id()).isNotBlank()
                    .isNotEqualTo(Shelf.create("Another", preset, 2).id());
        }

        @ParameterizedTest(name = "\"{0}\" is not a title")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aShelfNeedsATitle(String title) {
            assertThatThrownBy(() -> Shelf.create(title, aPresetSearchFor("KITCHEN"), 1))
                    .isInstanceOf(ShelfTitleRequiredException.class);
        }

        @Test
        void shelvesAreOrderedFromTheFirstShelf() {
            assertThatThrownBy(() -> Shelf.create("Weekend quick refresh", aPresetSearchFor("KITCHEN"), 0))
                    .isInstanceOf(InvalidShelfOrderException.class)
                    .hasMessageContaining("0");
        }
    }
}
