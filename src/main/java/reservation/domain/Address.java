package reservation.domain;

import kernel.City;
import kernel.GeoPoint;

/** Where the job is: the customer's address as it was when they requested, with its pin and city. */
public record Address(String line, GeoPoint pin, City city) {
}
