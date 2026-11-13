package reservation.domain.exception;

import kernel.City;

public class CityNotCoveredException extends RuntimeException {

    public CityNotCoveredException(City city) {
        super("The provider does not cover " + city);
    }
}
