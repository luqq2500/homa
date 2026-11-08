package offering.domain;

/** A city a provider covers, such as Cyberjaya. */
public record City(String name) {

    public City {
        name = OfferingText.require(name, "A city");
    }

    public static City of(String name) {
        return new City(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
