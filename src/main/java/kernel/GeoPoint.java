package kernel;

/** A point on the map, such as the pin of an address. */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (!(latitude >= -90 && latitude <= 90)) {
            throw new IllegalArgumentException("Latitude is from -90 to 90: " + latitude);
        }
        if (!(longitude >= -180 && longitude <= 180)) {
            throw new IllegalArgumentException("Longitude is from -180 to 180: " + longitude);
        }
    }
}
