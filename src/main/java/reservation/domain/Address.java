package reservation.domain;

import java.util.Objects;

/** Where the job is: the customer's street address and its city. */
public record Address(String street, City city) {

    public Address {
        street = ReservationText.require(street, "A street address");
        Objects.requireNonNull(city);
    }

    public static Address of(String street, City city) {
        return new Address(street, city);
    }
}
