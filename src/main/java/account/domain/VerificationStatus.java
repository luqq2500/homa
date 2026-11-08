package account.domain;

/**
 * Where a provider's eKYC stands. Everyone starts {@code PENDING} until Kenal returns a result, and a
 * result from Kenal is {@code VERIFIED} or {@code REJECTED}.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /** Re-verification was missed: the provider takes no new requests until they pass. Never a result from Kenal. */
    LAPSED
}
