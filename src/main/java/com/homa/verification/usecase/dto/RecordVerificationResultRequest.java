package com.homa.verification.usecase.dto;

/**
 * The verification service's webhook, as {@code infra/transport} read it once its signature checked out.
 *
 * @param notificationId   the service's id for this notification, so a repeated webhook is a no-op
 * @param serviceReference how the service knows the check
 */
public record RecordVerificationResultRequest(String notificationId, String serviceReference) {
}
