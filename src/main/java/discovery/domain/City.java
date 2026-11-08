package discovery.domain;

/** A city, such as Cyberjaya. */
public record City(String name) {

    public City {
        name = DiscoveryText.require(name, "A city");
    }

    public static City of(String name) {
        return new City(name);
    }
}
