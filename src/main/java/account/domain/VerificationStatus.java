package account.domain;

/**
 * Where a business's verification stands, as the KYC provider reports it. A business starts
 * {@code PENDING}; a result is {@code VERIFIED} or {@code REJECTED}; the KYC provider reports
 * {@code LAPSED} when a re-verification is missed.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /** Re-verification was missed: the business takes no new requests until it passes. Never a result. */
    LAPSED
}
