package account.domain;

/** Where a helper stands with a provider's team. Only a {@code MEMBER} is on the team. */
public enum TeamStatus {
    /** Invited; eKYC is pending. Not on the team. */
    INVITED,
    /** Invited, but eKYC failed. Not on the team; the helper can redo eKYC. */
    INVITE_REJECTED,
    /** eKYC verified: on the team, shown to customers, counted in the team size, and can be named for jobs. */
    MEMBER,
    /** The invite was withdrawn or the member was removed. Cannot come back. */
    REMOVED
}
