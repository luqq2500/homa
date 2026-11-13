package catalogue.usecase;

import static catalogue.support.CatalogueWorld.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.FeaturedSearchNotFoundException;
import catalogue.support.CatalogueWorld;
import catalogue.usecase.dto.FeatureSearchRequest;
import catalogue.usecase.dto.FeatureSearchResponse;
import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;
import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;
import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.ViewShowcaseResponse.Search;
import catalogue.usecase.exception.JobNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShowcaseUsecasesTest {

    private final CatalogueWorld world = new CatalogueWorld();
    private final String kitchen = world.group("Kitchen");
    private final String kitchenCare = world.job("Kitchen care", kitchen);
    private final String laundry = world.job("Laundry folding", kitchen);
    private final GuestViewShowcase viewer = new GuestViewShowcase(world.showcases);

    @Test
    void aFeaturedSearchIsShownWithItsJobsDurationAndHelpers() {
        FeatureSearchResponse response = new AdminFeatureSearch(world.showcases, world.jobs)
                .feature(new FeatureSearchRequest(ADMIN, "Weekend quick refresh", Set.of(kitchenCare, laundry), 3, 2));

        assertThat(response.place()).isEqualTo(1);
        assertThat(viewer.view().searches()).containsExactly(
                new Search(response.searchId(), "Weekend quick refresh", Set.of(kitchenCare, laundry), 3, 2));
    }

    @Test
    void aSearchIsOnlyFeaturedForJobsOnTheList() {
        AdminFeatureSearch usecase = new AdminFeatureSearch(world.showcases, world.jobs);

        assertThatThrownBy(() -> usecase.feature(new FeatureSearchRequest(ADMIN, "Ghost", Set.of("no-job"), 2, 1)))
                .isInstanceOf(JobNotFoundException.class);
        assertThat(viewer.view().searches()).isEmpty();
    }

    @Test
    void aFeaturedSearchIsRenamed() {
        String searchId = world.featured("Weekend refresh", kitchenCare);

        RenameFeaturedSearchResponse response = new AdminRenameFeaturedSearch(world.showcases)
                .rename(new RenameFeaturedSearchRequest(ADMIN, searchId, "Weekend quick refresh"));

        assertThat(response).isEqualTo(new RenameFeaturedSearchResponse(searchId, "Weekend quick refresh"));
        assertThat(viewer.view().searches()).extracting(Search::name).containsExactly("Weekend quick refresh");
    }

    @Test
    void aFeaturedSearchIsMovedToItsNewPlace() {
        String first = world.featured("First", kitchenCare);
        String second = world.featured("Second", kitchenCare);

        MoveFeaturedSearchResponse response = new AdminMoveFeaturedSearch(world.showcases)
                .move(new MoveFeaturedSearchRequest(ADMIN, second, 1));

        assertThat(response).isEqualTo(new MoveFeaturedSearchResponse(second, 1));
        assertThat(viewer.view().searches()).extracting(Search::searchId).containsExactly(second, first);
    }

    @Test
    void aRemovedFeaturedSearchIsNoLongerShown() {
        String first = world.featured("First", kitchenCare);
        String second = world.featured("Second", kitchenCare);
        AdminRemoveFeaturedSearch usecase = new AdminRemoveFeaturedSearch(world.showcases);

        RemoveFeaturedSearchResponse response = usecase.remove(new RemoveFeaturedSearchRequest(ADMIN, first));

        assertThat(response).isEqualTo(new RemoveFeaturedSearchResponse(first));
        assertThat(viewer.view().searches()).extracting(Search::searchId).containsExactly(second);
        assertThatThrownBy(() -> usecase.remove(new RemoveFeaturedSearchRequest(ADMIN, first)))
                .isInstanceOf(FeaturedSearchNotFoundException.class);
    }

    @Test
    void aFeaturedSearchLeftWithNoJobIsNotShown() {
        world.featured("Laundry day", laundry);
        String kitchenOnly = world.featured("Kitchen only", kitchenCare);

        new AdminRetireJob(world.jobs, world.showcases, world.publisher, world.clock)
                .retire(new RetireJobRequest(ADMIN, laundry, "No demand"));

        assertThat(viewer.view().searches()).extracting(Search::searchId).containsExactly(kitchenOnly);
    }
}
