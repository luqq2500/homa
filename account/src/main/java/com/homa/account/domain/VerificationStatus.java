package com.homa.account.domain;

/** Where an eKYC check stands. Everyone starts {@code PENDING} until Kenal returns a result. */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
