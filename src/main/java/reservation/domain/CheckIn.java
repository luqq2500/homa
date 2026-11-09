package reservation.domain;

import java.time.Instant;

/** The location check: when it passed, and how far the device was from the customer's location. */
public record CheckIn(Instant at, int distanceMetres) {
}
