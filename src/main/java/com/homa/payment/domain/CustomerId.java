package com.homa.payment.domain;

/** The customer whose payment is held, as Payment knows them. */
public record CustomerId(String value) {

    public CustomerId {
        Ids.requireText(value, "CustomerId");
    }
}
