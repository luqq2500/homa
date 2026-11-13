package catalogue.usecase;

import static catalogue.support.CatalogueWorld.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;

import catalogue.support.CatalogueWorld;
import catalogue.usecase.dto.RetireJobRequest;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.ViewCatalogueResponse;
import catalogue.usecase.dto.ViewCatalogueResponse.Group;
import catalogue.usecase.dto.ViewCatalogueResponse.Item;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderViewCatalogueTest {

    private final CatalogueWorld world = new CatalogueWorld();
    private final ProviderViewCatalogue usecase = new ProviderViewCatalogue(world.groups, world.jobs, world.tools);

    @Test
    void aProviderSeesTheJobsByGroupAndTheTools() {
        String kitchen = world.group("Kitchen");
        String livingRoom = world.group("Living room");
        String kitchenCare = world.job("Kitchen care", kitchen);
        String tidying = world.job("Tidying", livingRoom);
        String dishes = world.job("Dishes", kitchen);
        String vacuum = world.tool("Vacuum cleaner");

        ViewCatalogueResponse response = usecase.view();

        assertThat(response.groups()).containsExactly(
                new Group(kitchen, "Kitchen", List.of(new Item(kitchenCare, "Kitchen care"), new Item(dishes, "Dishes"))),
                new Group(livingRoom, "Living room", List.of(new Item(tidying, "Tidying"))));
        assertThat(response.tools()).containsExactly(new Item(vacuum, "Vacuum cleaner"));
    }

    @Test
    void retiredJobsAndToolsCanNoLongerBePicked() {
        String kitchen = world.group("Kitchen");
        String kitchenCare = world.job("Kitchen care", kitchen);
        String dishes = world.job("Dishes", kitchen);
        String vacuum = world.tool("Vacuum cleaner");
        String duster = world.tool("Feather duster");
        new AdminRetireJob(world.jobs, world.showcases, world.publisher, world.clock)
                .retire(new RetireJobRequest(ADMIN, dishes, "No demand"));
        new AdminRetireTool(world.tools, world.publisher, world.clock)
                .retire(new RetireToolRequest(ADMIN, duster, "Nobody brings one"));

        ViewCatalogueResponse response = usecase.view();

        assertThat(response.groups())
                .containsExactly(new Group(kitchen, "Kitchen", List.of(new Item(kitchenCare, "Kitchen care"))));
        assertThat(response.tools()).containsExactly(new Item(vacuum, "Vacuum cleaner"));
    }
}
