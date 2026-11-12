package account.usecase;

import account.domain.Address;
import account.domain.City;
import account.domain.Coordinates;
import account.usecase.exception.AddressNotFoundException;
import account.usecase.port.out.Geocoder;
import account.usecase.port.out.Location;

/** Turns the street the customer wrote into an address Homa can match by city and locate at check-in. */
final class Addresses {

    private Addresses() {
    }

    static Address locate(Geocoder geocoder, String street) {
        Location found = geocoder.locate(street).orElseThrow(() -> new AddressNotFoundException(street));
        return Address.of(street, City.of(found.city()), Coordinates.of(found.latitude(), found.longitude()));
    }
}
