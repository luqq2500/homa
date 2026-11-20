package com.homa.payment.domain;

/** The one check every Payment id makes: an id is never missing or blank. */
final class Ids {

    private Ids() {
    }

    static void requireText(String value, String idName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(idName + " must not be blank");
        }
    }
}
