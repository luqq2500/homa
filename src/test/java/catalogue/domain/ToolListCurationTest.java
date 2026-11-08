package catalogue.domain;

import static catalogue.domain.CatalogueBuilders.aToolList;
import static catalogue.domain.CatalogueBuilders.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.CatalogueNameRequiredException;
import catalogue.domain.exception.DuplicateToolCodeException;
import catalogue.domain.exception.ToolAlreadyRetiredException;
import catalogue.domain.exception.ToolNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tool list: the same rules as the job list. */
class ToolListCurationTest {

    @Nested
    class AddingTools {

        @Test
        void toolCodesAreUnique() {
            ToolList list = aToolList().with("STEAM_MOP").build();

            assertThatThrownBy(() -> list.add(tool("STEAM_MOP"), "Steam mop"))
                    .isInstanceOf(DuplicateToolCodeException.class)
                    .hasMessageContaining("STEAM_MOP");
        }

        @Test
        void aNewToolCanBePickedWithItsName() {
            ToolList list = ToolList.create();

            list.add(tool("STEAM_MOP"), " Steam mop ");

            assertThat(list.isOffered(tool("STEAM_MOP"))).isTrue();
            Tool added = list.tool(tool("STEAM_MOP")).orElseThrow();
            assertThat(added.code()).isEqualTo(tool("STEAM_MOP"));
            assertThat(added.name()).isEqualTo("Steam mop");
            assertThat(added.isActive()).isTrue();
        }

        @Test
        void aToolNeedsAName() {
            ToolList list = ToolList.create();

            assertThatThrownBy(() -> list.add(tool("STEAM_MOP"), null))
                    .isInstanceOf(CatalogueNameRequiredException.class)
                    .hasMessage("A tool needs a name");
            assertThat(list.contains(tool("STEAM_MOP"))).isFalse();
        }

        @Test
        void toolsAreListedInTheOrderHomaAddedThem() {
            ToolList list = aToolList().with("VACUUM", "STEAM_MOP").build();

            assertThat(list.tools()).extracting(Tool::code).containsExactly(tool("VACUUM"), tool("STEAM_MOP"));
            assertThatThrownBy(() -> list.tools().clear()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> list.offeredCodes().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void eachToolListHasItsOwnId() {
            assertThat(ToolList.create().id()).isNotBlank().isNotEqualTo(ToolList.create().id());
        }
    }

    @Nested
    class RenamingTools {

        @Test
        void aRenamedToolKeepsItsCode() {
            ToolList list = aToolList().with("STEAM_MOP").build();

            list.rename(tool("STEAM_MOP"), "Steam cleaner");

            assertThat(list.tool(tool("STEAM_MOP")).orElseThrow().name()).isEqualTo("Steam cleaner");
        }

        @Test
        void aRenamedToolStillNeedsAName() {
            ToolList list = aToolList().with("STEAM_MOP").build();

            assertThatThrownBy(() -> list.rename(tool("STEAM_MOP"), " "))
                    .isInstanceOf(CatalogueNameRequiredException.class);
        }

        @Test
        void onlyAToolOnTheListCanBeRenamed() {
            assertThatThrownBy(() -> ToolList.create().rename(tool("STEAM_MOP"), "Steam cleaner"))
                    .isInstanceOf(ToolNotFoundException.class)
                    .hasMessageContaining("STEAM_MOP");
        }
    }

    @Nested
    class RetiringTools {

        @Test
        void aRetiredToolStaysInTheListButCanNoLongerBePicked() {
            ToolList list = aToolList().with("STEAM_MOP", "VACUUM").build();

            list.retire(tool("STEAM_MOP"));

            assertThat(list.contains(tool("STEAM_MOP"))).isTrue();
            assertThat(list.isOffered(tool("STEAM_MOP"))).isFalse();
            assertThat(list.tool(tool("STEAM_MOP")).orElseThrow().isActive()).isFalse();
            assertThat(list.offeredCodes()).containsExactly(tool("VACUUM"));
        }

        @Test
        void aRetiredToolCodeCannotBeUsedAgain() {
            ToolList list = aToolList().with("STEAM_MOP").build();
            list.retire(tool("STEAM_MOP"));

            assertThatThrownBy(() -> list.add(tool("STEAM_MOP"), "Steam mop"))
                    .isInstanceOf(DuplicateToolCodeException.class);
        }

        @Test
        void aToolIsRetiredOnlyOnce() {
            ToolList list = aToolList().with("STEAM_MOP").build();
            list.retire(tool("STEAM_MOP"));

            assertThatThrownBy(() -> list.retire(tool("STEAM_MOP")))
                    .isInstanceOf(ToolAlreadyRetiredException.class)
                    .hasMessageContaining("STEAM_MOP");
        }

        @Test
        void onlyAToolOnTheListCanBeRetired() {
            assertThatThrownBy(() -> ToolList.create().retire(tool("STEAM_MOP")))
                    .isInstanceOf(ToolNotFoundException.class);
        }

        @Test
        void aToolNotOnTheListCannotBePicked() {
            assertThat(ToolList.create().isOffered(tool("LADDER"))).isFalse();
            assertThat(ToolList.create().tool(tool("LADDER"))).isEmpty();
        }
    }
}
