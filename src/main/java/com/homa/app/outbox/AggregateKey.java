package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

/**
 * The aggregate an event is about, which the outbox orders delivery by. By the {@link IntegrationEvent} convention
 * it is the record's component right after {@code eventId}: a String named {@code <something>Id}.
 *
 * @param type the component's name, such as {@code reservationId}
 * @param id   its value in this event
 */
record AggregateKey(String type, String id) {

    static AggregateKey of(IntegrationEvent event) {
        RecordComponent component = aggregateComponent(event.getClass());
        return new AggregateKey(component.getName(), valueOf(component, event));
    }

    private static RecordComponent aggregateComponent(Class<?> eventType) {
        RecordComponent[] components = eventType.getRecordComponents();
        if (components.length < 2 || !isAggregateId(components[1])) {
            throw new IllegalArgumentException(eventType.getSimpleName()
                    + " must name its aggregate in the component after eventId, a String ending in Id");
        }
        return components[1];
    }

    private static boolean isAggregateId(RecordComponent component) {
        String name = component.getName();
        return component.getType() == String.class && name.endsWith("Id") && !"eventId".equals(name);
    }

    private static String valueOf(RecordComponent component, IntegrationEvent event) {
        try {
            return (String) component.getAccessor().invoke(event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot read " + component.getName() + " of " + event, e);
        }
    }
}
