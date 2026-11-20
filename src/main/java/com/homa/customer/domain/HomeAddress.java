package com.homa.customer.domain;

/**
 * Where the customer lives: the street address and its city (CUS-1). Reservation copies the city to decide which
 * providers the customer can reserve (RES-1).
 */
public record HomeAddress(String streetAddress, CityCode city) {

    public HomeAddress {
        Required.text(streetAddress, "a home address");
        Required.detail(city, "a city in the home address");
    }
}
