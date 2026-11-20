package com.homa.provider.domain;

/** The result of a verification check: KYB for a business (PRV-1), KYC for a helper or supervisor (HLP-1–3). */
public enum VerificationOutcome {
    PASSED,
    FAILED;

    public boolean passed() {
        return this == PASSED;
    }
}
