package com.homa.customer.domain;

import com.homa.customer.domain.exception.MissingCustomerDetailException;

/** CUS-1's "must have": each customer detail is present, and a written one is not blank. */
final class Required {

    private Required() {
    }

    static <T> T detail(T value, String detail) {
        if (value == null) {
            throw new MissingCustomerDetailException(detail);
        }
        return value;
    }

    static void text(String value, String detail) {
        if (detail(value, detail).isBlank()) {
            throw new MissingCustomerDetailException(detail);
        }
    }
}
