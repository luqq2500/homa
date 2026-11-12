package account.usecase.port.out;

import java.util.Optional;

/** The geoservice: finds an address's city and coordinates, so Homa can match providers and locate check-in. */
public interface Geocoder {

    /** The address found, or empty when the geoservice cannot find it. */
    Optional<Location> locate(String street);
}
