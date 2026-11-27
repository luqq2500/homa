package com.homa.verification.usecase.port.out;

import java.util.Optional;

/**
 * The verification service (§11). It confirms facts only; Homa decides what follows (EXT-1). A check is
 * asynchronous: {@link #start} hands the person over to the service, which later calls Homa's webhook, and
 * {@link #resultOf} then reads the answer. No method returns a document image or biometric data, so none can reach
 * Homa (IDN-2, EXT-4). The client translates the vendor's response into these facts and decides nothing.
 */
public interface VerificationClient {

    /** Submits a check. The service knows it by the returned reference. */
    StartedVerification start(VerificationSubmission submission);

    /** The service's answer for the check it knows by {@code reference}; empty while it hasn't answered. */
    Optional<VerificationAnswer> resultOf(String reference);
}
