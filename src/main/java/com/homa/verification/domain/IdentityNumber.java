package com.homa.verification.domain;

import com.homa.verification.domain.exception.MissingIdentityDetailException;
import java.util.Locale;

/**
 * An identity document number (IDN-2). It is used only to recognise a person and to check payout accounts (IDN-3),
 * so it is compared without spaces or hyphens and in upper case: "880101-14-5678" and "880101145678" are the same
 * MyKad. {@link #toString()} never shows it, so it can't leak into a log or a message.
 *
 * <p>{@link #value()} is the one way to read the number. Only storing it, Support's recorded view (IDN-3, DAT-14)
 * and the payout-account holder check may call it; it never goes into an event, a response or a log.
 */
public record IdentityNumber(String value) {

    private static final String DETAIL = "an identity document number";

    public IdentityNumber {
        if (value == null) {
            throw new MissingIdentityDetailException(DETAIL);
        }
        value = value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new MissingIdentityDetailException(DETAIL);
        }
    }

    /** IDN-3: the number is withheld. */
    @Override
    public String toString() {
        return "IdentityNumber[withheld]";
    }
}
