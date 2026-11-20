package com.homa.app.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homa.ie.port.IntegrationEvent;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The outbox payload: an event record as JSON, and its type as the record's simple name. Only records in
 * {@code com.homa.ie.events} that implement {@link IntegrationEvent} are written or read back, so a row can never
 * make the relay load an arbitrary class. Uses its own mapper, so the web layer's JSON settings never change what is
 * stored.
 */
@Component
public class IntegrationEventJson {

    static final String EVENTS_PACKAGE = "com.homa.ie.events";
    private static final Pattern EVENT_TYPE = Pattern.compile("[A-Z][A-Za-z0-9]*IE");

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addModule(new KernelJsonModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** The stored type name of an event, such as {@code PaymentHeldIE}. */
    public String typeOf(IntegrationEvent event) {
        Class<?> type = event.getClass();
        if (!EVENTS_PACKAGE.equals(type.getPackageName()) || !type.isRecord()) {
            throw new IllegalArgumentException(type.getName() + " is not an integration event of " + EVENTS_PACKAGE);
        }
        return type.getSimpleName();
    }

    public String write(IntegrationEvent event) {
        typeOf(event);
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write " + event.getClass().getSimpleName() + " as JSON", e);
        }
    }

    public IntegrationEvent read(String eventType, String payload) {
        Class<? extends IntegrationEvent> type = eventClass(eventType);
        try {
            return mapper.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read " + eventType + " from its payload", e);
        }
    }

    private static Class<? extends IntegrationEvent> eventClass(String eventType) {
        if (!EVENT_TYPE.matcher(eventType).matches()) {
            throw new IllegalArgumentException("Not an integration event type: " + eventType);
        }
        Class<?> type = load(EVENTS_PACKAGE + "." + eventType);
        if (!type.isRecord() || !IntegrationEvent.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(type.getName() + " is not an integration event record");
        }
        return type.asSubclass(IntegrationEvent.class);
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, IntegrationEventJson.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No integration event " + className, e);
        }
    }
}
