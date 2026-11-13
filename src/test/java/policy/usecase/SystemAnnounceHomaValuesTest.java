package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import ie.events.HomaValuesChangedIE;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import policy.domain.HomaValue;
import policy.domain.HomaValues;
import policy.support.InMemoryHomaValuesRepository;
import policy.support.RecordingPolicyIEPublisher;

class SystemAnnounceHomaValuesTest {

    private static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");

    private final InMemoryHomaValuesRepository values = new InMemoryHomaValuesRepository();
    private final RecordingPolicyIEPublisher publisher = new RecordingPolicyIEPublisher();
    private final SystemAnnounceHomaValues usecase =
            new SystemAnnounceHomaValues(values, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void theLaunchValuesAreAnnouncedWhenNothingHasChanged() {
        assertThat(usecase.announce().version()).isEqualTo(1);

        assertThat(publisher.published()).singleElement().isInstanceOfSatisfying(HomaValuesChangedIE.class, event -> {
            assertThat(event.version()).isEqualTo(1);
            assertThat(event.serviceFee()).isEqualTo(Percentage.of(15));
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void theVersionInForceIsAnnouncedAfterAChange() {
        values.save(HomaValues.launch().changed(HomaValue.SERVICE_FEE, 12));

        assertThat(usecase.announce().version()).isEqualTo(2);

        assertThat(publisher.published()).singleElement().isInstanceOfSatisfying(HomaValuesChangedIE.class,
                event -> assertThat(event.serviceFee()).isEqualTo(Percentage.of(12)));
    }
}
