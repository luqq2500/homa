package com.homa.verification.usecase.port.out;

/**
 * The verification service took a check.
 *
 * @param reference how the service knows the check; its webhook names it
 * @param handoff   what the app opens to hand the person over to the service, such as a session link
 */
public record StartedVerification(String reference, String handoff) {
}
