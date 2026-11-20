package com.homa.app.outbox;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * JSON for the kernel types an event may carry. The kernel stays JDK-only, so its mapping lives here. Amounts are
 * written as decimal strings so they come back exact, scale included: {@code {"amount":"39.375","currency":"MYR"}}
 * and {@code "15.5"} for a percentage.
 */
final class KernelJsonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    KernelJsonModule() {
        super("homa-kernel");
        addSerializer(Money.class, new MoneySerializer());
        addDeserializer(Money.class, new MoneyDeserializer());
        addSerializer(Percentage.class, new PercentageSerializer());
        addDeserializer(Percentage.class, new PercentageDeserializer());
    }

    private static final class MoneySerializer extends StdSerializer<Money> {

        private static final long serialVersionUID = 1L;

        MoneySerializer() {
            super(Money.class);
        }

        @Override
        public void serialize(Money money, JsonGenerator json, SerializerProvider provider) throws IOException {
            json.writeStartObject();
            json.writeStringField("amount", money.amount().toPlainString());
            json.writeStringField("currency", money.currency().getCurrencyCode());
            json.writeEndObject();
        }
    }

    private static final class MoneyDeserializer extends StdDeserializer<Money> {

        private static final long serialVersionUID = 1L;

        MoneyDeserializer() {
            super(Money.class);
        }

        @Override
        public Money deserialize(JsonParser json, DeserializationContext context) throws IOException {
            JsonNode node = json.readValueAsTree();
            return Money.of(new BigDecimal(node.required("amount").asText()),
                    Currency.getInstance(node.required("currency").asText()));
        }
    }

    private static final class PercentageSerializer extends StdSerializer<Percentage> {

        private static final long serialVersionUID = 1L;

        PercentageSerializer() {
            super(Percentage.class);
        }

        @Override
        public void serialize(Percentage percentage, JsonGenerator json, SerializerProvider provider)
                throws IOException {
            json.writeString(percentage.value().toPlainString());
        }
    }

    private static final class PercentageDeserializer extends StdDeserializer<Percentage> {

        private static final long serialVersionUID = 1L;

        PercentageDeserializer() {
            super(Percentage.class);
        }

        @Override
        public Percentage deserialize(JsonParser json, DeserializationContext context) throws IOException {
            return Percentage.of(json.getValueAsString());
        }
    }
}
