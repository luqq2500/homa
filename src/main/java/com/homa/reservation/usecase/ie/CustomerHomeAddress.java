package com.homa.reservation.usecase.ie;

import com.homa.reservation.domain.ServiceAddress;

/**
 * The home address Customer's events let Reservation know. They carry only the city (RES-1), not the street address
 * or the arrival note a reservation keeps (RES-8, CUS-1): until they do, the city stands in for the street address
 * and the arrival note is empty.
 */
final class CustomerHomeAddress {

    private CustomerHomeAddress() {
    }

    static ServiceAddress inCity(String city) {
        return new ServiceAddress(city, city, "");
    }
}
