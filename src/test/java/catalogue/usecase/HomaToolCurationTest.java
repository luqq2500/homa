package catalogue.usecase;

import static catalogue.usecase.CatalogueUsecaseFixtures.CLOCK;
import static catalogue.usecase.CatalogueUsecaseFixtures.FOUNDER;
import static catalogue.usecase.CatalogueUsecaseFixtures.NOW;
import static catalogue.usecase.CatalogueUsecaseFixtures.recorded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.ToolCode;
import catalogue.domain.exception.DuplicateToolCodeException;
import catalogue.domain.exception.ToolAlreadyRetiredException;
import catalogue.domain.exception.ToolNotFoundException;
import catalogue.usecase.CatalogueUsecaseFixtures.AuditLog;
import catalogue.usecase.CatalogueUsecaseFixtures.InMemoryToolLists;
import catalogue.usecase.CatalogueUsecaseFixtures.PublishedEvents;
import catalogue.usecase.dto.AddToolRequest;
import catalogue.usecase.dto.AddToolResponse;
import catalogue.usecase.dto.RenameToolRequest;
import catalogue.usecase.dto.RenameToolResponse;
import catalogue.usecase.dto.RetireToolRequest;
import catalogue.usecase.dto.RetireToolResponse;
import catalogue.usecase.exception.AdminReasonRequiredException;
import catalogue.usecase.exception.DecidedByRequiredException;
import catalogue.usecase.port.out.AdminAction;
import ie.events.ToolAddedIE;
import ie.events.ToolRenamedIE;
import ie.events.ToolRetiredIE;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Homa's founders curate the tool list: they add, rename and retire tools. */
class HomaToolCurationTest {

    private final InMemoryToolLists toolLists = new InMemoryToolLists();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();
    private final HomaAddTool addTool = new HomaAddTool(toolLists, events, auditLog, CLOCK);
    private final HomaRenameTool renameTool = new HomaRenameTool(toolLists, events, auditLog, CLOCK);
    private final HomaRetireTool retireTool = new HomaRetireTool(toolLists, events, auditLog, CLOCK);

    private void theSteamMopIsOnTheList() {
        addTool.add(new AddToolRequest(FOUNDER, "STEAM_MOP", "Steam mop"));
    }

    private boolean isOffered(String code) {
        return toolLists.find().orElseThrow().isOffered(ToolCode.of(code));
    }

    @Nested
    class Adding {

        @Test
        void anAddedToolIsSavedAndProvidersCanSayTheyBringIt() {
            AddToolResponse added = addTool.add(new AddToolRequest(FOUNDER, "STEAM_MOP", "Steam mop"));

            assertThat(added).isEqualTo(new AddToolResponse("STEAM_MOP", "Steam mop"));
            assertThat(added.code()).isEqualTo("STEAM_MOP");
            assertThat(added.name()).isEqualTo("Steam mop");
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(isOffered("STEAM_MOP")).isTrue();
        }

        @Test
        void anAddedToolIsToldToTheOtherContexts() {
            theSteamMopIsOnTheList();

            assertThat(events.all).containsExactly(new ToolAddedIE("STEAM_MOP", "Steam mop", NOW));
        }

        @Test
        void anAddedToolIsRecordedWithWhoAddedItAndWhen() {
            theSteamMopIsOnTheList();

            assertThat(auditLog.all).containsExactly(recorded("ADD_TOOL", "STEAM_MOP"));
            AdminAction action = auditLog.all.get(0);
            assertThat(action.decidedBy()).isEqualTo(FOUNDER);
            assertThat(action.action()).isEqualTo("ADD_TOOL");
            assertThat(action.subject()).isEqualTo("STEAM_MOP");
            assertThat(action.reason()).isEmpty();
            assertThat(action.at()).isEqualTo(NOW);
        }

        @Test
        void aSecondToolJoinsTheSameToolList() {
            theSteamMopIsOnTheList();
            String listId = toolLists.find().orElseThrow().id();

            addTool.add(new AddToolRequest(FOUNDER, "VACUUM", "Vacuum cleaner"));

            assertThat(toolLists.find().orElseThrow().id()).isEqualTo(listId);
            assertThat(isOffered("STEAM_MOP")).isTrue();
            assertThat(isOffered("VACUUM")).isTrue();
            assertThat(toolLists.saves).isEqualTo(2);
        }

        @Test
        void aToolCodeAlreadyOnTheListIsRefusedAndNothingIsSavedToldOrRecorded() {
            theSteamMopIsOnTheList();

            assertThatThrownBy(() -> addTool.add(new AddToolRequest(FOUNDER, "STEAM_MOP", "Steam mop pro")))
                    .isInstanceOf(DuplicateToolCodeException.class);

            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @ParameterizedTest(name = "decided by \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void addingAToolNeedsTheFounderWhoDecidedIt(String decidedBy) {
            assertThatThrownBy(() -> addTool.add(new AddToolRequest(decidedBy, "STEAM_MOP", "Steam mop")))
                    .isInstanceOf(DecidedByRequiredException.class)
                    .hasMessage("An admin action needs the founder who decided it");

            assertThat(toolLists.find()).isEmpty();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void theFoundersIdIsRecordedWithoutSpacesAroundIt() {
            addTool.add(new AddToolRequest("  " + FOUNDER + " ", "STEAM_MOP", "Steam mop"));

            assertThat(auditLog.all).containsExactly(recorded("ADD_TOOL", "STEAM_MOP"));
        }
    }

    @Nested
    class Renaming {

        @Test
        void aRenamedToolIsSavedWithItsNewName() {
            theSteamMopIsOnTheList();

            RenameToolResponse renamed = renameTool.rename(new RenameToolRequest(FOUNDER, "STEAM_MOP", "Steam cleaner"));

            assertThat(renamed).isEqualTo(new RenameToolResponse("STEAM_MOP", "Steam cleaner"));
            assertThat(renamed.code()).isEqualTo("STEAM_MOP");
            assertThat(renamed.name()).isEqualTo("Steam cleaner");
            assertThat(toolLists.saves).isEqualTo(2);
            assertThat(toolLists.find().orElseThrow().tool(ToolCode.of("STEAM_MOP")).orElseThrow().name()).isEqualTo("Steam cleaner");
        }

        @Test
        void aRenameIsToldToTheOtherContextsSoTheNameTheyShowIsUpdated() {
            theSteamMopIsOnTheList();

            renameTool.rename(new RenameToolRequest(FOUNDER, "STEAM_MOP", "Steam cleaner"));

            assertThat(events.all).hasSize(2).last().isEqualTo(new ToolRenamedIE("STEAM_MOP", "Steam cleaner", NOW));
            assertThat(auditLog.all).containsExactly(recorded("ADD_TOOL", "STEAM_MOP"), recorded("RENAME_TOOL", "STEAM_MOP"));
        }

        @Test
        void renamingAToolToItsOwnNameSavesTellsAndRecordsNothing() {
            theSteamMopIsOnTheList();

            RenameToolResponse renamed = renameTool.rename(new RenameToolRequest(FOUNDER, "STEAM_MOP", "Steam mop"));

            assertThat(renamed).isEqualTo(new RenameToolResponse("STEAM_MOP", "Steam mop"));
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void aToolThatIsNotOnTheListIsNotRenamed() {
            assertThatThrownBy(() -> renameTool.rename(new RenameToolRequest(FOUNDER, "STEAM_MOP", "Steam cleaner")))
                    .isInstanceOf(ToolNotFoundException.class);

            assertThat(toolLists.saves).isZero();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void renamingAToolNeedsTheFounderWhoDecidedIt() {
            theSteamMopIsOnTheList();

            assertThatThrownBy(() -> renameTool.rename(new RenameToolRequest(" ", "STEAM_MOP", "Steam cleaner")))
                    .isInstanceOf(DecidedByRequiredException.class);

            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(toolLists.find().orElseThrow().tool(ToolCode.of("STEAM_MOP")).orElseThrow().name()).isEqualTo("Steam mop");
        }
    }

    @Nested
    class Retiring {

        @Test
        void aRetiredToolIsSavedAndProvidersCanNoLongerSayTheyBringIt() {
            theSteamMopIsOnTheList();

            RetireToolResponse retired = retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "No provider brings it"));

            assertThat(retired).isEqualTo(new RetireToolResponse("STEAM_MOP", NOW));
            assertThat(retired.code()).isEqualTo("STEAM_MOP");
            assertThat(retired.retiredAt()).isEqualTo(NOW);
            assertThat(toolLists.saves).isEqualTo(2);
            assertThat(isOffered("STEAM_MOP")).isFalse();
        }

        @Test
        void aRetiredToolIsToldToTheOtherContextsSoItComesOffTheListings() {
            theSteamMopIsOnTheList();

            retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "No provider brings it"));

            assertThat(events.all).last().isEqualTo(new ToolRetiredIE("STEAM_MOP", NOW));
            assertThat(events.all).hasSize(2);
        }

        @Test
        void aRetirementIsRecordedWithItsReason() {
            theSteamMopIsOnTheList();

            retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "  No provider brings it "));

            assertThat(auditLog.all).last()
                    .isEqualTo(new AdminAction(FOUNDER, "RETIRE_TOOL", "STEAM_MOP", "No provider brings it", NOW));
        }

        @ParameterizedTest(name = "reason \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void retiringAToolNeedsAReason(String reason) {
            theSteamMopIsOnTheList();

            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", reason)))
                    .isInstanceOf(AdminReasonRequiredException.class)
                    .hasMessage("A reason is required to retire a tool");

            assertThat(isOffered("STEAM_MOP")).isTrue();
            assertThat(toolLists.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void retiringAToolNeedsTheFounderWhoDecidedIt() {
            theSteamMopIsOnTheList();

            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest(null, "STEAM_MOP", "Replaced")))
                    .isInstanceOf(DecidedByRequiredException.class);

            assertThat(isOffered("STEAM_MOP")).isTrue();
        }

        @Test
        void aToolIsRetiredOnlyOnce() {
            theSteamMopIsOnTheList();
            retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "No provider brings it"));

            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "Again")))
                    .isInstanceOf(ToolAlreadyRetiredException.class);

            assertThat(toolLists.saves).isEqualTo(2);
            assertThat(events.all).hasSize(2);
            assertThat(auditLog.all).hasSize(2);
        }

        @Test
        void aToolThatIsNotOnTheListIsNotRetired() {
            assertThatThrownBy(() -> retireTool.retire(new RetireToolRequest(FOUNDER, "STEAM_MOP", "Replaced")))
                    .isInstanceOf(ToolNotFoundException.class);

            assertThat(toolLists.saves).isZero();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }
    }
}
