package com.homa.provider.usecase.exception;

/** The provider has no team member with this id. A member of another provider's team is not found either. */
public final class TeamMemberNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TeamMemberNotFoundException(String memberId) {
        super("No team member " + memberId + " in the provider's team");
    }
}
