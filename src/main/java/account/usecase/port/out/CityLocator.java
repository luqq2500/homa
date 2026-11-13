package account.usecase.port.out;

import java.util.Optional;
import kernel.City;
import kernel.GeoPoint;

/** The geoservice that tells which of Homa's cities a pin is in, if any. */
public interface CityLocator {

    Optional<City> cityAt(GeoPoint pin);
}
