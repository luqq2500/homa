package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.VerificationAttempt;
import java.util.Optional;

/** Verifications people started, pending or decided. A refused verification stays here as its record. */
public interface VerificationAttemptRepository {

    /** The verification the verification service knows by {@code serviceReference}. */
    Optional<VerificationAttempt> findByServiceReference(String serviceReference);

    void save(VerificationAttempt attempt);
}
