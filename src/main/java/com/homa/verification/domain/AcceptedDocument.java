package com.homa.verification.domain;

import java.util.Objects;

/**
 * One row of a country's identity documents (§14): the document, the status it verifies, who holds it, and whether
 * its expiry date is used as the holder's work permit expiry. In Malaysia citizens hold a MyKad and permanent
 * residents a MyPR, both local; foreign workers hold an i-Kad, whose date is used as the permit's expiry.
 *
 * @param holders         who holds the document, in lower case, for example "permanent residents"
 * @param datesWorkPermit true when the document's expiry is the holder's work permit expiry (HLP-3)
 */
public record AcceptedDocument(IdentityDocumentType type, PersonStatus status, String holders, boolean datesWorkPermit) {

    public AcceptedDocument {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(holders, "holders");
    }
}
