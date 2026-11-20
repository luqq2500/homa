package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * One example of every record in the integration event catalog, built from its components, so an event added to
 * {@code com.homa.ie.events} is covered by the outbox tests without editing them. A component type the outbox has
 * never stored fails here first.
 */
final class ExampleEvents {

    static final Money AMOUNT = Money.myr("39.375");
    static final Percentage RATE = Percentage.of("15.5");
    static final Instant AT = Instant.parse("2026-12-05T02:00:00Z");

    private ExampleEvents() {
    }

    static List<Class<?>> catalog() {
        return new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(IntegrationEventJson.EVENTS_PACKAGE).stream()
                .filter(JavaClass::isRecord)
                .<Class<?>>map(JavaClass::reflect)
                .sorted((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()))
                .toList();
    }

    static IntegrationEvent of(Class<?> eventType) {
        RecordComponent[] components = eventType.getRecordComponents();
        Object[] values = Arrays.stream(components).map(ExampleEvents::valueFor).toArray();
        Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        try {
            Constructor<?> canonical = eventType.getDeclaredConstructor(types);
            return (IntegrationEvent) canonical.newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build an example " + eventType.getSimpleName(), e);
        }
    }

    private static Object valueFor(RecordComponent component) {
        Class<?> type = component.getType();
        if (type == String.class) {
            return component.getName() + "-1";
        }
        if (type == Money.class) {
            return AMOUNT;
        }
        if (type == Percentage.class) {
            return RATE;
        }
        if (type == Instant.class) {
            return AT;
        }
        if (type == Duration.class) {
            return Duration.ofMinutes(90);
        }
        if (type == int.class) {
            return 3;
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == List.class && elementType(component, 0) == String.class) {
            return List.of("SALAK_TINGGI", "CYBERJAYA");
        }
        if (type == Map.class && elementType(component, 1) == Money.class) {
            return Map.of("CYBERJAYA", Money.myr("30.00"));
        }
        throw new IllegalStateException("No example value for " + component + "; teach the outbox JSON this type");
    }

    private static Type elementType(RecordComponent component, int index) {
        return ((ParameterizedType) component.getGenericType()).getActualTypeArguments()[index];
    }
}
