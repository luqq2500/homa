package com.homa.account.domain;

import com.homa.account.domain.exception.InvalidCoordinatesException;

/** Where an address is, from the geoservice. Check-in compares the provider's device against it. */
public record Coordinates(double latitude, double longitude) {

    public Coordinates {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new InvalidCoordinatesException(latitude, longitude);
        }
    }

    public static Coordinates of(double latitude, double longitude) {
        return new Coordinates(latitude, longitude);
    }
}
