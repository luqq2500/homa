package com.homa.account.domain;

/** The city of an address, such as Cyberjaya. Search matches it against the cities a provider covers. */
public record City(String name) {

    public City {
        name = AccountText.require(name, "A city");
    }

    public static City of(String name) {
        return new City(name);
    }
}
