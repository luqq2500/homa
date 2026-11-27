package com.homa.verification.usecase.dto;

/**
 * A person starts identity verification.
 *
 * @param signInId       the signed-in user, as {@code infra/security} reads them from their token
 * @param role           CUSTOMER, OWNER or MEMBER
 * @param accountId      the customerId when role is CUSTOMER (CUS-3); null for OWNER and MEMBER
 * @param documentType   the identity document the person chose, as the country calls it, such as "MyKad"
 * @param documentNumber the number as the person typed it, or null when the service reads it from the document
 */
public record StartVerificationRequest(
        String signInId, String role, String accountId, String documentType, String documentNumber) {

    /** Withholds the number (IDN-3). */
    @Override
    public String toString() {
        return "StartVerificationRequest[signInId=" + signInId + ", role=" + role + ", accountId=" + accountId
                + ", documentType=" + documentType + "]";
    }
}
