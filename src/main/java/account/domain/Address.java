package account.domain;

import java.util.Objects;

/**
 * A customer's address: the street as the customer wrote it, its city, and its coordinates from the
 * geoservice. Homa uses the city to match providers and the coordinates to locate check-in.
 */
public record Address(String street, City city, Coordinates coordinates) {

    public Address {
        street = AccountText.require(street, "A street address");
        Objects.requireNonNull(city);
        Objects.requireNonNull(coordinates);
    }

    public static Address of(String street, City city, Coordinates coordinates) {
        return new Address(street, city, coordinates);
    }
}
