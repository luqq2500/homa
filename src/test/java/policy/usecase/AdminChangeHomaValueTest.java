package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ie.events.HomaValuesChangedIE;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import policy.domain.HomaValue;
import policy.domain.HomaValues;
import policy.domain.ValueChange;
import policy.domain.exception.ChangeReasonMissingException;
import policy.domain.exception.HomaValueRuleBrokenException;
import policy.support.InMemoryHomaValuesRepository;
import policy.support.InMemoryValueChangeRepository;
import policy.support.RecordingPolicyIEPublisher;
import policy.usecase.dto.ChangeHomaValueRequest;
import policy.usecase.dto.ChangeHomaValueResponse;
import policy.usecase.exception.UnknownHomaValueException;

class AdminChangeHomaValueTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");

    private final InMemoryHomaValuesRepository values = new InMemoryHomaValuesRepository();
    private final InMemoryValueChangeRepository changes = new InMemoryValueChangeRepository();
    private final RecordingPolicyIEPublisher publisher = new RecordingPolicyIEPublisher();
    private final AdminChangeHomaValue usecase =
            new AdminChangeHomaValue(values, changes, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void theFirstChangeStartsFromTheLaunchValues() {
        ChangeHomaValueResponse response =
                usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));

        assertThat(response).isEqualTo(new ChangeHomaValueResponse(2, "SERVICE_FEE", 12));
        assertThat(values.current()).contains(HomaValues.launch().changed(HomaValue.SERVICE_FEE, 12));
    }

    @Test
    void aLaterChangeBuildsOnTheVersionInForce() {
        usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));
        ChangeHomaValueResponse response =
                usecase.change(new ChangeHomaValueRequest("admin-2", "FEE_WINDOW", 720, "Shorter window"));

        assertThat(response).isEqualTo(new ChangeHomaValueResponse(3, "FEE_WINDOW", 720));
        assertThat(values.current().orElseThrow().amountOf(HomaValue.SERVICE_FEE)).isEqualTo(12);
    }

    @Test
    void theChangeIsRecordedWithItsReasonAdminAndTime() {
        usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));

        assertThat(changes.all()).singleElement().satisfies(change -> {
            assertThat(change.value()).isEqualTo(HomaValue.SERVICE_FEE);
            assertThat(change.from()).isEqualTo(15);
            assertThat(change.to()).isEqualTo(12);
            assertThat(change.version()).isEqualTo(2);
            assertThat(change.reason()).isEqualTo("Launch promotion");
            assertThat(change.adminId()).isEqualTo("admin-1");
            assertThat(change.changedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void theOtherContextsAreToldTheWholeNewSetOfValues() {
        usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));

        assertThat(publisher.published()).singleElement().isEqualTo(new HomaValuesChangedIE(
                2, Percentage.of(12), 1, 6, Duration.ofHours(2), Duration.ofDays(30), Duration.ofHours(6),
                Duration.ofMinutes(15), Duration.ofMinutes(15), 300, Duration.ofMinutes(15), Percentage.of(30),
                Duration.ofHours(1), Duration.ofHours(24), Duration.ofHours(1), Duration.ofMinutes(30),
                Duration.ofHours(24), Duration.ofDays(120), Duration.ofHours(24), Duration.ofHours(24),
                Percentage.of(10), Percentage.of(20), Percentage.of(20), Percentage.of(30), Percentage.of(50),
                Duration.ofDays(14), 3, NOW));
    }

    @Test
    void aChangeThatBreaksARuleChangesNothingAndTellsNoOne() {
        assertThatThrownBy(() -> usecase.change(new ChangeHomaValueRequest("admin-1", "RESPONSE_DEADLINE", 120, "Longer")))
                .isInstanceOf(HomaValueRuleBrokenException.class);

        assertThat(values.current()).isEmpty();
        assertThat(changes.all()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    void aChangeWithoutAReasonChangesNothing() {
        assertThatThrownBy(() -> usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "")))
                .isInstanceOf(ChangeReasonMissingException.class);

        assertThat(values.current()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    void onlyValuesHomaSetsCanBeChanged() {
        assertThatThrownBy(() -> usecase.change(new ChangeHomaValueRequest("admin-1", "HOURLY_RATE", 12, "Why not")))
                .isInstanceOf(UnknownHomaValueException.class);
        assertThatThrownBy(() -> usecase.change(new ChangeHomaValueRequest("admin-1", null, 12, "Why not")))
                .isInstanceOf(UnknownHomaValueException.class);
    }

    @Test
    void savedChangesKeepTheirOrder() {
        usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 12, "Launch promotion"));
        usecase.change(new ChangeHomaValueRequest("admin-1", "SERVICE_FEE", 15, "Promotion over"));

        assertThat(changes.all()).extracting(ValueChange::to).containsExactly(12L, 15L);
    }
}
