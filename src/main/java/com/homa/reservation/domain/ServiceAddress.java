package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.IncompleteBookingException;
import java.util.Objects;

/**
 * The customer's home address as it was when the reservation was made: the address, its city (RES-1) and the
 * arrival note the team reads on arrival (CUS-1).
 */
public record ServiceAddress(String addressLine, String city, String arrivalNote) {

    public ServiceAddress {
        Objects.requireNonNull(addressLine, "addressLine");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(arrivalNote, "arrivalNote");
        if (addressLine.isBlank() || city.isBlank()) {
            throw new IncompleteBookingException("a home address with its city");
        }
    }
}
