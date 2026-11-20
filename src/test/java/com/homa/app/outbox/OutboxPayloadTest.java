package com.homa.app.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.port.IntegrationEvent;
import com.homa.kernel.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OutboxPayloadTest {

    private final IntegrationEventJson json = new IntegrationEventJson();

    static Stream<Class<?>> catalog() {
        return ExampleEvents.catalog().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("catalog")
    @DisplayName("Every event in the catalog comes back from the outbox equal to what was published.")
    void everyCatalogEventSurvivesTheOutbox(Class<?> eventType) {
        IntegrationEvent published = ExampleEvents.of(eventType);

        IntegrationEvent relayed = json.read(json.typeOf(published), json.write(published));

        assertThat(relayed).isEqualTo(published);
    }

    @Test
    @DisplayName("The catalog is not empty, so the round trip above checks something.")
    void catalogHasEvents() {
        assertThat(ExampleEvents.catalog()).contains(PaymentHeldIE.class, ListingTermsChangedIE.class);
    }

    @Test
    @DisplayName("An amount keeps its exact value and scale through the outbox (PAY-8 rounds later, not here).")
    void moneyKeepsItsExactAmount() {
        PaymentHeldIE held = new PaymentHeldIE("event-1", "checkout-1", Money.myr("39.375"), ExampleEvents.AT,
                ExampleEvents.AT);

        String payload = json.write(held);
        PaymentHeldIE relayed = (PaymentHeldIE) json.read("PaymentHeldIE", payload);

        assertThat(payload).contains("{\"amount\":\"39.375\",\"currency\":\"MYR\"}");
        assertThat(relayed.amount().amount()).isEqualTo(new BigDecimal("39.375"));
    }

    @Test
    @DisplayName("A percentage is stored as an exact decimal string.")
    void percentageIsStoredAsADecimalString() {
        ListingTermsChangedIE terms = (ListingTermsChangedIE) ExampleEvents.of(ListingTermsChangedIE.class);

        assertThat(json.write(terms)).contains("\"discountRate\":\"15.5\"");
    }

    @Test
    @DisplayName("A missing amount stays missing.")
    void absentMoneyStaysAbsent() {
        PaymentHeldIE held = new PaymentHeldIE("event-1", "checkout-1", null, ExampleEvents.AT, ExampleEvents.AT);

        assertThat(json.read("PaymentHeldIE", json.write(held))).isEqualTo(held);
    }

    @Test
    @DisplayName("A field the record no longer has is ignored, so old rows still relay.")
    void unknownFieldsAreIgnored() {
        String payload = "{\"eventId\":\"e\",\"checkoutId\":\"c\",\"reason\":\"r\",\"occurredAt\":\"2026-12-05T02:00:00Z\","
                + "\"retiredField\":1}";

        assertThat(json.read("PaymentFailedIE", payload)).isNotNull();
    }

    @Test
    @DisplayName("A type name that is not an event simple name is refused.")
    void typeNamesOutsideTheCatalogPatternAreRefused() {
        assertThatThrownBy(() -> json.read("java.lang.Runtime", "{}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Not an integration event type");
    }

    @Test
    @DisplayName("A type the catalog does not have is refused.")
    void unknownEventTypesAreRefused() {
        assertThatThrownBy(() -> json.read("NoSuchThingIE", "{}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No integration event");
    }

    @Test
    @DisplayName("A class in the events package that is not an integration event is refused.")
    void nonEventsInTheEventsPackageAreRefused() {
        assertThatThrownBy(() -> json.read("NotAnEventIE", "{\"eventId\":\"e\"}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("is not an integration event record");
    }

    @Test
    @DisplayName("An unreadable payload is refused.")
    void unreadablePayloadsAreRefused() {
        assertThatThrownBy(() -> json.read("PaymentFailedIE", "not json"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot read PaymentFailedIE");
    }

    @Test
    @DisplayName("Only catalog records are written to the outbox.")
    void eventsOutsideTheCatalogAreNotWritten() {
        IntegrationEvent stray = new StrayIE("event-1", Instant.EPOCH);

        assertThatThrownBy(() -> json.write(stray)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an integration event of com.homa.ie.events");
    }

    @Test
    @DisplayName("An event that is not a record is not written either.")
    void nonRecordEventsAreNotWritten() {
        IntegrationEvent anonymous = new IntegrationEvent() {
            @Override
            public String eventId() {
                return "event-1";
            }

            @Override
            public Instant occurredAt() {
                return Instant.EPOCH;
            }
        };

        assertThatThrownBy(() -> json.typeOf(anonymous)).isInstanceOf(IllegalArgumentException.class);
    }

    private record StrayIE(String eventId, Instant occurredAt) implements IntegrationEvent {
    }
}
