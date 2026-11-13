package reservation.usecase.port.out;

import kernel.GeoPoint;

/** The geoservice's location check at check-in. The device's location is used only at that moment. */
public interface LocationChecker {

    boolean isWithin(GeoPoint device, GeoPoint home, int radiusMetres);
}
