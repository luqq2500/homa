package catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.FeaturedSearchInvalidException;
import catalogue.domain.exception.FeaturedSearchNotFoundException;
import catalogue.domain.exception.JobRetiredException;
import catalogue.domain.exception.NameMissingException;
import catalogue.domain.exception.PlaceOutsideShowcaseException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShowcaseTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    private final JobGroup kitchen = JobGroup.add("Kitchen");
    private final Job kitchenCare = Job.add("Kitchen care", kitchen);
    private final Job laundry = Job.add("Laundry folding", kitchen);
    private final Showcase showcase = Showcase.empty();

    private FeaturedSearch feature(String name, Job... jobs) {
        return showcase.feature(name, List.of(jobs), 2, 1);
    }

    @Test
    void aFeaturedSearchKeepsItsJobsDurationAndHelpers() {
        FeaturedSearch search = showcase.feature(" Weekend quick refresh ", List.of(kitchenCare, laundry), 3, 2);

        assertThat(search.id()).isNotBlank();
        assertThat(search.name()).isEqualTo("Weekend quick refresh");
        assertThat(search.jobIds()).containsExactlyInAnyOrder(kitchenCare.id(), laundry.id());
        assertThat(search.hours()).isEqualTo(3);
        assertThat(search.helpers()).isEqualTo(2);
        assertThat(search.isShown()).isTrue();
    }

    @Test
    void aNewFeaturedSearchTakesTheLastPlace() {
        FeaturedSearch first = feature("Weekend quick refresh", kitchenCare);
        FeaturedSearch second = feature("Laundry day", laundry);

        assertThat(showcase.searches()).containsExactly(first, second);
        assertThat(showcase.placeOf(first.id())).isEqualTo(1);
        assertThat(showcase.placeOf(second.id())).isEqualTo(2);
    }

    @Test
    void aFeaturedSearchNeedsANameAJobAnHourAndAHelper() {
        assertThatThrownBy(() -> feature(" ", kitchenCare)).isInstanceOf(NameMissingException.class);
        assertThatThrownBy(() -> feature("Empty")).isInstanceOf(FeaturedSearchInvalidException.class);
        assertThatThrownBy(() -> showcase.feature("No time", List.of(kitchenCare), 0, 1))
                .isInstanceOf(FeaturedSearchInvalidException.class);
        assertThatThrownBy(() -> showcase.feature("Nobody", List.of(kitchenCare), 1, 0))
                .isInstanceOf(FeaturedSearchInvalidException.class);
        assertThat(showcase.feature("Smallest", List.of(kitchenCare), 1, 1).isShown()).isTrue();
        assertThat(showcase.searches()).hasSize(1);
    }

    @Test
    void aFeaturedSearchSearchesOnlyForJobsProvidersCanOffer() {
        laundry.retire("No demand", NOW);

        assertThatThrownBy(() -> feature("Laundry day", kitchenCare, laundry)).isInstanceOf(JobRetiredException.class);
        assertThat(showcase.searches()).isEmpty();
    }

    @Test
    void aFeaturedSearchCanBeRenamedInPlace() {
        FeaturedSearch first = feature("Weekend quick refresh", kitchenCare);
        FeaturedSearch second = feature("Laundry day", laundry);

        FeaturedSearch renamed = showcase.rename(second.id(), "Laundry morning");

        assertThat(renamed.name()).isEqualTo("Laundry morning");
        assertThat(renamed.id()).isEqualTo(second.id());
        assertThat(renamed.jobIds()).isEqualTo(second.jobIds());
        assertThat(showcase.searches()).containsExactly(first, renamed);
        assertThatThrownBy(() -> showcase.rename(first.id(), "")).isInstanceOf(NameMissingException.class);
    }

    @Test
    void movingASearchGivesItItsNewPlaceAndMovesTheOthersAlong() {
        FeaturedSearch a = feature("A", kitchenCare);
        FeaturedSearch b = feature("B", kitchenCare);
        FeaturedSearch c = feature("C", kitchenCare);

        showcase.move(c.id(), 1);
        assertThat(showcase.searches()).containsExactly(c, a, b);

        showcase.move(c.id(), 3);
        assertThat(showcase.searches()).containsExactly(a, b, c);

        showcase.move(a.id(), 2);
        assertThat(showcase.searches()).containsExactly(b, a, c);
    }

    @Test
    void aSearchCannotMoveToAPlaceTheShowcaseDoesNotHave() {
        FeaturedSearch a = feature("A", kitchenCare);
        FeaturedSearch b = feature("B", kitchenCare);

        assertThatThrownBy(() -> showcase.move(a.id(), 0)).isInstanceOf(PlaceOutsideShowcaseException.class);
        assertThatThrownBy(() -> showcase.move(a.id(), 3)).isInstanceOf(PlaceOutsideShowcaseException.class);
        assertThat(showcase.searches()).containsExactly(a, b);
    }

    @Test
    void aRemovedSearchLeavesTheShowcase() {
        FeaturedSearch a = feature("A", kitchenCare);
        FeaturedSearch b = feature("B", kitchenCare);

        showcase.remove(a.id());

        assertThat(showcase.searches()).containsExactly(b);
        assertThat(showcase.placeOf(b.id())).isEqualTo(1);
    }

    @Test
    void aSearchThatIsNotInTheShowcaseCannotBeChanged() {
        feature("A", kitchenCare);

        assertThatThrownBy(() -> showcase.rename("other", "B")).isInstanceOf(FeaturedSearchNotFoundException.class);
        assertThatThrownBy(() -> showcase.move("other", 1)).isInstanceOf(FeaturedSearchNotFoundException.class);
        assertThatThrownBy(() -> showcase.remove("other")).isInstanceOf(FeaturedSearchNotFoundException.class);
        assertThatThrownBy(() -> showcase.placeOf("other")).isInstanceOf(FeaturedSearchNotFoundException.class);
    }

    @Test
    void aRetiredJobComesOffEveryFeaturedSearch() {
        FeaturedSearch both = feature("Weekend quick refresh", kitchenCare, laundry);
        FeaturedSearch other = feature("Kitchen only", kitchenCare);

        showcase.retireJob(laundry.id());

        assertThat(showcase.searches().get(0).jobIds()).containsExactly(kitchenCare.id());
        assertThat(showcase.searches().get(0).id()).isEqualTo(both.id());
        assertThat(showcase.searches().get(1)).isEqualTo(other);
    }

    @Test
    void aFeaturedSearchLeftWithNoJobIsNoLongerShownButKeepsItsPlace() {
        FeaturedSearch laundryDay = feature("Laundry day", laundry);
        FeaturedSearch kitchenOnly = feature("Kitchen only", kitchenCare);

        showcase.retireJob(laundry.id());

        assertThat(showcase.shown()).containsExactly(kitchenOnly);
        assertThat(showcase.searches()).hasSize(2);
        assertThat(showcase.searches().get(0).isShown()).isFalse();
        assertThat(showcase.placeOf(laundryDay.id())).isEqualTo(1);
    }

    @Test
    void aStoredShowcaseKeepsItsOrder() {
        FeaturedSearch a = feature("A", kitchenCare);
        FeaturedSearch b = feature("B", kitchenCare);

        Showcase stored = new Showcase(List.of(b, a));

        assertThat(stored.searches()).containsExactly(b, a);
        assertThat(stored.shown()).containsExactly(b, a);
    }
}
