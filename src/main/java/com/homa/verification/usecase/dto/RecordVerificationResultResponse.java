package com.homa.verification.usecase.dto;

/**
 * Where a verification stands after the service's webhook.
 *
 * @param outcome        VERIFIED, REFUSED, PENDING (the service hasn't answered yet) or DUPLICATE (this
 *                       notification was already handled)
 * @param verificationId Homa's id for the verification; null for DUPLICATE
 * @param personId       the person; null for DUPLICATE
 * @param role           CUSTOMER, OWNER or MEMBER; null for DUPLICATE
 * @param refusedUnder   for REFUSED, the rule that refused it, such as "IDN-3" or "CUS-10"; null otherwise
 * @param reason         for REFUSED, why, in the rule's words; never an identity number. Null otherwise
 */
public record RecordVerificationResultResponse(
        String outcome, String verificationId, String personId, String role, String refusedUnder, String reason) {

    /** The outcome of a notification already handled. */
    public static final String DUPLICATE = "DUPLICATE";
}
