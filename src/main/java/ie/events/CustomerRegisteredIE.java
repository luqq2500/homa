package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import kernel.City;
import kernel.GeoPoint;

/** A customer registered, with the details the other contexts use. */
public record CustomerRegisteredIE(String customerId, String name, String addressLine, GeoPoint pin, City city, String accessNotes, String language, Instant occurredAt) implements AccountIE {}
