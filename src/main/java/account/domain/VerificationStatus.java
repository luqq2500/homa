package account.domain;

/**
 * Where a provider's eKYC, or a business's eKYB, stands. Everyone starts {@code PENDING} until the
 * verification provider returns a result, and a result is {@code VERIFIED} or {@code REJECTED}.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /** Re-verification was missed: the provider takes no new requests until they pass. Never a result from Kenal. */
    LAPSED
}
