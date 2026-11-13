package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import kernel.City;
import kernel.GeoPoint;

/** A customer changed their name, address, access notes or language. Requests and reservations already made keep what they were made with. */
public record CustomerDetailsChangedIE(String customerId, String name, String addressLine, GeoPoint pin, City city, String accessNotes, String language, Instant occurredAt) implements AccountIE {}
