package com.homa.payment.domain;

/** The provider a {@link Payout} is released to, as Payment knows it. */
public record ProviderId(String value) {

    public ProviderId {
        Ids.requireText(value, "ProviderId");
    }
}
