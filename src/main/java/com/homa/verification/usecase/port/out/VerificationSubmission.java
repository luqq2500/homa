package com.homa.verification.usecase.port.out;

/**
 * What Homa sends the verification service to start a check: its own id for the verification, which the service
 * echoes back, and the document details the app collected. The service reads the document itself, so its images
 * never pass through Homa (EXT-4).
 *
 * @param documentType   the document as the country policy names it, such as "MyKad"
 * @param documentNumber the number as the person typed it, or null when the service reads it from the document
 */
public record VerificationSubmission(String verificationId, String documentType, String documentNumber) {

    /** Withholds the number (IDN-3). */
    @Override
    public String toString() {
        return "VerificationSubmission[verificationId=" + verificationId + ", documentType=" + documentType + "]";
    }
}
