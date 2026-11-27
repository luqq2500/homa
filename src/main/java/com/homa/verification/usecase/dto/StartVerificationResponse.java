package com.homa.verification.usecase.dto;

/**
 * A verification started and waits for the verification service.
 *
 * @param verificationId Homa's id for the verification
 * @param personId       the person, as every context knows them
 * @param handoff        what the app opens to hand the person over to the verification service
 * @param state          PENDING
 */
public record StartVerificationResponse(String verificationId, String personId, String handoff, String state) {
}
