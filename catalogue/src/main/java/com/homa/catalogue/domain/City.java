package com.homa.catalogue.domain;

/** A city Homa may serve, such as Cyberjaya. */
public record City(String name) {

    public City {
        name = CatalogueText.requireName(name, "A city");
    }

    public static City of(String name) {
        return new City(name);
    }
}
