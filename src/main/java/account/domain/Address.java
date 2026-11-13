package account.domain;

import account.domain.exception.AddressIncompleteException;
import kernel.City;
import kernel.GeoPoint;

/** A customer's address: its text, the pin the customer confirmed on the map, and the city the pin is in. */
public record Address(String line, GeoPoint pin, City city) {

    public Address {
        if (line == null || line.isBlank() || pin == null || city == null) {
            throw new AddressIncompleteException();
        }
        line = line.strip();
    }
}
