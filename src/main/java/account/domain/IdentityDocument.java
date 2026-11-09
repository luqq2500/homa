package account.domain;

import java.util.Objects;

/**
 * The identity document a person verified with, as read from the eKYC result. The registered name is the
 * name on the document: nobody types or changes it.
 */
public record IdentityDocument(Country issuingCountry, DocumentType type, String registeredName) {

    public IdentityDocument {
        Objects.requireNonNull(issuingCountry);
        Objects.requireNonNull(type);
        registeredName = AccountText.require(registeredName, "A registered name");
    }
}
