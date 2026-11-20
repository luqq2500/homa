package com.homa.customer.domain.exception;

/**
 * CUS-1: a customer must have a verified phone number, a user name, a home address with city, and an arrival note.
 * Raised when one of them is missing or blank, at registration or on a later profile change.
 */
public class MissingCustomerDetailException extends RuntimeException {

    private final String detail;

    public MissingCustomerDetailException(String detail) {
        super("CUS-1: a customer must have " + detail);
        this.detail = detail;
    }

    /** The missing detail, for example "a user name". */
    public String detail() {
        return detail;
    }
}
