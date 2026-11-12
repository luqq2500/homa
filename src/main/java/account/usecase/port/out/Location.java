package account.usecase.port.out;

import java.util.Objects;

/** Where the geoservice found an address: its city, and its latitude and longitude. */
public record Location(String city, double latitude, double longitude) {

    public Location {
        Objects.requireNonNull(city, "city");
    }
}
