package com.homa.verification.usecase.exception;

/** A verification was asked for a role that isn't CUSTOMER, OWNER or MEMBER. */
public final class UnknownRoleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownRoleException(String role) {
        super("A person verifies as CUSTOMER, OWNER or MEMBER, not " + role);
    }
}
