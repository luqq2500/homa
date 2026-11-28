package com.homa.rating.domain;

/** The one check every Rating id makes: an id is never missing or blank. */
final class Ids {

    private Ids() {
    }

    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
