package catalogue.usecase;

import static catalogue.support.CatalogueWorld.ADMIN;
import static catalogue.support.CatalogueWorld.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.Retirement;
import catalogue.domain.exception.ToolRetiredException;
import catalogue.support.CatalogueWorld;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import catalogue.usecase.exception.ToolNotFoundException;
import ie.events.ToolAddedIE;
import ie.events.ToolRetiredIE;
import org.junit.jupiter.api.Test;

class ToolListUsecasesTest {

    private final CatalogueWorld world = new CatalogueWorld();

    @Test
    void anAddedToolIsSavedAndProvidersCanNowBringIt() {
        String toolId = world.tool("Vacuum cleaner");

        assertThat(world.tools.find(toolId).orElseThrow().name()).isEqualTo("Vacuum cleaner");
        assertThat(world.publisher.published()).containsExactly(new ToolAddedIE(toolId, "Vacuum cleaner", NOW));
    }

    @Test
    void aToolIsRenamed() {
        String toolId = world.tool("Vacuum");
        AdminRenameTool usecase = new AdminRenameTool(world.tools);

        RenameToolResponse response = usecase.rename(new RenameToolRequest(ADMIN, toolId, "Vacuum cleaner"));

        assertThat(response).isEqualTo(new RenameToolResponse(toolId, "Vacuum cleaner"));
        assertThat(world.tools.find(toolId).orElseThrow().name()).isEqualTo("Vacuum cleaner");
        assertThat(world.tools.timesSaved(toolId)).isEqualTo(2);
        assertThatThrownBy(() -> usecase.rename(new RenameToolRequest(ADMIN, "none", "X")))
                .isInstanceOf(ToolNotFoundException.class);
    }

    @Test
    void aRetiredToolIsKeptAndTheOtherContextsAreTold() {
        String toolId = world.tool("Feather duster");
        AdminRetireTool usecase = new AdminRetireTool(world.tools, world.publisher, world.clock);

        RetireToolResponse response = usecase.retire(new RetireToolRequest(ADMIN, toolId, "Nobody brings one"));

        assertThat(response).isEqualTo(new RetireToolResponse(toolId, NOW));
        assertThat(world.tools.find(toolId).orElseThrow().retirement()).contains(new Retirement("Nobody brings one", NOW));
        assertThat(world.tools.timesSaved(toolId)).isEqualTo(2);
        assertThat(world.publisher.published(ToolRetiredIE.class))
                .containsExactly(new ToolRetiredIE(toolId, "Nobody brings one", NOW));
        assertThatThrownBy(() -> usecase.retire(new RetireToolRequest(ADMIN, toolId, "Again")))
                .isInstanceOf(ToolRetiredException.class);
        assertThatThrownBy(() -> usecase.retire(new RetireToolRequest(ADMIN, "none", "Gone")))
                .isInstanceOf(ToolNotFoundException.class);
        assertThat(world.publisher.published(ToolRetiredIE.class)).hasSize(1);
    }
}
