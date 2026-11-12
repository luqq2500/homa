package account.usecase.port.out;

/**
 * The KYC provider (R9). Homa opens its pages and it reports each result back by webhook; Homa sets no time
 * limits and works out no due dates. Each method returns the address of the KYC provider's page to open on
 * the provider's phone.
 */
public interface IdentityVerificationService {

    /** KYB for the business, with its owner's or director's KYC in the same flow; also its re-verification. */
    String startKyb(String providerId);
}
