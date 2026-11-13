package policy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import policy.domain.exception.ChangeReasonMissingException;

class ValueChangeTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    private final HomaValues before = HomaValues.launch();
    private final HomaValues after = before.changed(HomaValue.SERVICE_FEE, 12);

    @Test
    void aChangeRecordsWhatChangedWhyWhoAndWhen() {
        ValueChange change = ValueChange.record(before, after, HomaValue.SERVICE_FEE, "Launch promotion", "admin-1", NOW);

        assertThat(change.id()).isNotBlank();
        assertThat(change.value()).isEqualTo(HomaValue.SERVICE_FEE);
        assertThat(change.from()).isEqualTo(15);
        assertThat(change.to()).isEqualTo(12);
        assertThat(change.version()).isEqualTo(2);
        assertThat(change.reason()).isEqualTo("Launch promotion");
        assertThat(change.adminId()).isEqualTo("admin-1");
        assertThat(change.changedAt()).isEqualTo(NOW);
    }

    @Test
    void everyChangeGetsItsOwnId() {
        assertThat(new ValueChangeId().generate()).isNotEqualTo(new ValueChangeId().generate());
    }

    @Test
    void aValueIsNotChangedWithoutAReason() {
        assertThatThrownBy(() -> ValueChange.record(before, after, HomaValue.SERVICE_FEE, " ", "admin-1", NOW))
                .isInstanceOf(ChangeReasonMissingException.class);
        assertThatThrownBy(() -> ValueChange.record(before, after, HomaValue.SERVICE_FEE, null, "admin-1", NOW))
                .isInstanceOf(ChangeReasonMissingException.class);
    }
}
