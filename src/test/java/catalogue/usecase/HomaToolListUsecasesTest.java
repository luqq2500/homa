package catalogue.usecase;

import static catalogue.usecase.CatalogueFakes.CLOCK;
import static catalogue.usecase.CatalogueFakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.ToolCode;
import catalogue.domain.ToolList;
import catalogue.domain.exception.DuplicateToolCodeException;
import catalogue.domain.exception.ToolAlreadyRetiredException;
import catalogue.domain.exception.ToolNotFoundException;
import catalogue.usecase.CatalogueFakes.InMemoryToolLists;
import catalogue.usecase.CatalogueFakes.RecordingPublisher;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import ie.events.ToolAddedIE;
import ie.events.ToolRetiredIE;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Homa curates its tool list: adding, renaming and retiring tools. */
class HomaToolListUsecasesTest {

    private final InMemoryToolLists toolLists = new InMemoryToolLists();
    private final RecordingPublisher events = new RecordingPublisher();

    private InMemoryToolLists aToolListWith(String... codes) {
        ToolList list = ToolList.create();
        for (String code : codes) {
            list.add(ToolCode.of(code), "Tool " + code);
        }
        return toolLists.holding(list);
    }

    @Nested
    class HomaAddsATool {

        private final HomaAddTool addTool = new HomaAddTool(toolLists, events, CLOCK);

        @Test
        void theFirstToolStartsTheToolListAndProvidersAreToldTheyCanBringIt() {
            AddToolResponse response = addTool.add(new AddToolRequest("STEAM_MOP", " Steam mop "));

            assertThat(response).isEqualTo(new AddToolResponse("STEAM_MOP", "Steam mop"));
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(toolLists.stored.isOffered(ToolCode.of("STEAM_MOP"))).isTrue();
            assertThat(events.published).containsExactly(new ToolAddedIE("STEAM_MOP", "Steam mop", NOW));
        }

        @Test
        void aToolIsAddedToTheExistingToolList() {
            ToolList existing = aToolListWith("VACUUM").stored;

            addTool.add(new AddToolRequest("STEAM_MOP", "Steam mop"));

            assertThat(toolLists.stored).isSameAs(existing);
            assertThat(existing.offeredCodes()).containsExactlyInAnyOrder(ToolCode.of("VACUUM"), ToolCode.of("STEAM_MOP"));
        }

        @Test
        void aRejectedToolIsNeitherSavedNorAnnounced() {
            aToolListWith("STEAM_MOP");

            assertThatThrownBy(() -> addTool.add(new AddToolRequest("STEAM_MOP", "Again")))
                    .isInstanceOf(DuplicateToolCodeException.class);
            assertThat(toolLists.saves).isZero();
            assertThat(events.published).isEmpty();
        }
    }

    @Nested
    class HomaRenamesATool {

        private final HomaRenameTool renameTool = new HomaRenameTool(toolLists);

        @Test
        void aRenamedToolIsSavedWithItsNewName() {
            aToolListWith("STEAM_MOP");

            RenameToolResponse response = renameTool.rename(new RenameToolRequest("STEAM_MOP", "Steam cleaner"));

            assertThat(response).isEqualTo(new RenameToolResponse("STEAM_MOP", "Steam cleaner"));
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(toolLists.stored.tool(ToolCode.of("STEAM_MOP")).orElseThrow().name()).isEqualTo("Steam cleaner");
        }

        @Test
        void onlyAToolOnTheListCanBeRenamed() {
            assertThatThrownBy(() -> renameTool.rename(new RenameToolRequest("STEAM_MOP", "Steam cleaner")))
                    .isInstanceOf(ToolNotFoundException.class);
            assertThat(toolLists.saves).isZero();
        }
    }

    @Nested
    class HomaRetiresATool {

        private final HomaRetireTool retireTool = new HomaRetireTool(toolLists, events, CLOCK);

        @Test
        void aRetiredToolIsSavedAndProvidersAreToldTheyCanNoLongerBringIt() {
            aToolListWith("STEAM_MOP");

            RetireToolResponse response = retireTool.retire(new RetireToolRequest("STEAM_MOP"));

            assertThat(response).isEqualTo(new RetireToolResponse("STEAM_MOP", NOW));
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(toolLists.stored.isOffered(ToolCode.of("STEAM_MOP"))).isFalse();
            assertThat(events.published).containsExactly(new ToolRetiredIE("STEAM_MOP", NOW));
        }

        @Test
        void aToolRetiredTwiceIsNeitherSavedNorAnnouncedAgain() {
            aToolListWith("STEAM_MOP").stored.retire(ToolCode.of("STEAM_MOP"));

            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest("STEAM_MOP")))
                    .isInstanceOf(ToolAlreadyRetiredException.class);
            assertThat(toolLists.saves).isZero();
            assertThat(events.published).isEmpty();
        }

        @Test
        void onlyAToolOnTheListCanBeRetired() {
            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest("STEAM_MOP")))
                    .isInstanceOf(ToolNotFoundException.class);
        }
    }
}
