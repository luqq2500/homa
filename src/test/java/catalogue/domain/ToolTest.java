package catalogue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import catalogue.domain.exception.NameMissingException;
import catalogue.domain.exception.RetirementReasonMissingException;
import catalogue.domain.exception.ToolRetiredException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ToolTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");

    @Test
    void aNewToolIsOnTheListWithItsOwnCode() {
        Tool tool = Tool.add(" Vacuum cleaner ");

        assertThat(tool.id()).isNotBlank().isNotEqualTo(Tool.add("Vacuum cleaner").id());
        assertThat(tool.name()).isEqualTo("Vacuum cleaner");
        assertThat(tool.isRetired()).isFalse();
        assertThat(tool.retirement()).isEmpty();
        assertThatThrownBy(() -> Tool.add(" ")).isInstanceOf(NameMissingException.class);
    }

    @Test
    void renamingAToolKeepsItsCode() {
        Tool tool = Tool.add("Vacuum");
        String code = tool.id();

        tool.rename("Vacuum cleaner");

        assertThat(tool.name()).isEqualTo("Vacuum cleaner");
        assertThat(tool.id()).isEqualTo(code);
        assertThatThrownBy(() -> tool.rename(null)).isInstanceOf(NameMissingException.class);
    }

    @Test
    void retiringAToolKeepsItWithTheReasonAndTime() {
        Tool tool = Tool.add("Feather duster");

        assertThatThrownBy(() -> tool.retire("", NOW)).isInstanceOf(RetirementReasonMissingException.class);
        tool.retire("Nobody brings one", NOW);

        assertThat(tool.isRetired()).isTrue();
        assertThat(tool.retirement()).contains(new Retirement("Nobody brings one", NOW));
    }

    @Test
    void aRetiredToolCanNoLongerBeChanged() {
        Tool tool = Tool.add("Feather duster");
        tool.retire("Nobody brings one", NOW);

        assertThatThrownBy(() -> tool.rename("Duster")).isInstanceOf(ToolRetiredException.class);
        assertThatThrownBy(() -> tool.retire("Again", NOW.plusSeconds(1))).isInstanceOf(ToolRetiredException.class);
        assertThat(tool.name()).isEqualTo("Feather duster");
        assertThat(tool.retirement()).contains(new Retirement("Nobody brings one", NOW));
    }
}
