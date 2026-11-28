package com.homa.rating.domain;

/** The provider of a reservation, as Rating knows it. Provider issues it. */
public record ProviderId(String value) {

    public ProviderId {
        Ids.requireText(value, "ProviderId");
    }
}
