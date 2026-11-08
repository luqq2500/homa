package com.homa.offering.domain;

import com.homa.offering.domain.exception.OfferingDetailRequiredException;

/** Text on a listing is required and kept without surrounding spaces. */
final class OfferingText {

    private OfferingText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new OfferingDetailRequiredException(what);
        }
        return text.strip();
    }
}
