package reservation.domain;

/** The city of the customer's address, such as Cyberjaya. */
public record City(String name) {

    public City {
        name = ReservationText.require(name, "A city");
    }

    public static City of(String name) {
        return new City(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
