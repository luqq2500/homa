package reservation.domain;

import java.time.Instant;

/** When the lead checked in, and how far the device was from the customer's location. */
public record CheckIn(Instant at, int distanceMetres) {
}
