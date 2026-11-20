package com.homa.app.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.port.IntegrationEvent;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OutboxOrderingKeyTest {

    static Stream<Class<?>> catalog() {
        return ExampleEvents.catalog().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalog")
    @DisplayName("Every event in the catalog names its aggregate right after its eventId.")
    void everyCatalogEventNamesItsAggregate(Class<?> eventType) {
        AggregateKey key = AggregateKey.of(ExampleEvents.of(eventType));

        assertThat(key.type()).endsWith("Id").isNotEqualTo("eventId");
        assertThat(key.id()).isEqualTo(key.type() + "-1");
    }

    @Test
    @DisplayName("A payment is ordered by its checkout.")
    void paymentEventsAreOrderedByCheckout() {
        PaymentHeldIE held = new PaymentHeldIE("event-1", "checkout-7", ExampleEvents.AMOUNT, ExampleEvents.AT,
                ExampleEvents.AT);

        assertThat(AggregateKey.of(held)).isEqualTo(new AggregateKey("checkoutId", "checkout-7"));
    }

    @Test
    @DisplayName("A completed job is ordered by its reservation.")
    void reservationEventsAreOrderedByReservation() {
        IntegrationEvent completed = ExampleEvents.of(ReservationCompletedIE.class);

        assertThat(AggregateKey.of(completed).type()).isEqualTo("reservationId");
    }

    @Test
    @DisplayName("An event with nothing after its eventId cannot be ordered.")
    void eventWithoutAnAggregateIsRefused() {
        assertThatThrownBy(() -> AggregateKey.of(new OnlyIdIE("event-1")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must name its aggregate");
    }

    @Test
    @DisplayName("An aggregate must be named by a String id.")
    void aggregateMustBeAStringId() {
        assertThatThrownBy(() -> AggregateKey.of(new NumberedIE("event-1", 3, Instant.EPOCH)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AggregateKey.of(new CityFirstIE("event-1", "CYBERJAYA", Instant.EPOCH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("The eventId itself is not an aggregate.")
    void eventIdIsNotAnAggregate() {
        assertThatThrownBy(() -> AggregateKey.of(new SwappedIE(Instant.EPOCH, "event-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record OnlyIdIE(String eventId) implements IntegrationEvent {
        @Override
        public Instant occurredAt() {
            return Instant.EPOCH;
        }
    }

    private record NumberedIE(String eventId, int count, Instant occurredAt) implements IntegrationEvent {
    }

    private record CityFirstIE(String eventId, String city, Instant occurredAt) implements IntegrationEvent {
    }

    private record SwappedIE(Instant occurredAt, String eventId) implements IntegrationEvent {
    }
}
