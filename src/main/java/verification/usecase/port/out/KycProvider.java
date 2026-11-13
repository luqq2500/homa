package verification.usecase.port.out;

/** The KYC provider. It runs every check on its own pages; Homa only starts them and records their results. */
public interface KycProvider {

    /** Starts the business's KYB or its re-verification; returns the session to open. */
    String startBusinessCheck(String businessId);

    /** Starts a person's check, or their re-verification, on the provider's phone; returns the session to open. */
    String startTeamMemberCheck(String memberId, String profilePhoto);

    /** Asks for an early re-verification of a business or a team member. */
    void requestEarlyRecheck(String subjectId, String reason);
}
