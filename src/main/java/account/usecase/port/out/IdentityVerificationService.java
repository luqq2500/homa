package account.usecase.port.out;

/**
 * The KYC provider (R9). Homa opens its pages and it reports each result back by webhook; Homa sets no time
 * limits and works out no due dates. The {@code start} methods return the address of the KYC provider's page
 * to open on the provider's phone, with the person present.
 */
public interface IdentityVerificationService {

    /** KYB for the business, with its owner's or director's KYC in the same flow; also its re-verification. */
    String startKyb(String providerId);

    /** The on-the-spot KYC of a person being added to a team. */
    String startMemberCheck(String verificationId);

    /** A team member's re-verification. */
    String startMemberReverification(String memberId);

    /** Asks for an early re-verification of the business, with Homa's reason. */
    void requestBusinessRecheck(String providerId, String reason);

    /** Asks for an early re-verification of a team member, with Homa's reason. */
    void requestMemberRecheck(String memberId, String reason);
}
