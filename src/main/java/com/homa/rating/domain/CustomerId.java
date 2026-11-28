package com.homa.rating.domain;

/** The customer of a reservation, as Rating knows them. Customer issues it. */
public record CustomerId(String value) {

    public CustomerId {
        Ids.requireText(value, "CustomerId");
    }
}
