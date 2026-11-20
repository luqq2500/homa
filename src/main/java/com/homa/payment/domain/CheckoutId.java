package com.homa.payment.domain;

/** The checkout (RES-11) a {@link PaymentHold} is opened for, as Payment knows it. Reservation issues it. */
public record CheckoutId(String value) {

    public CheckoutId {
        Ids.requireText(value, "CheckoutId");
    }
}
