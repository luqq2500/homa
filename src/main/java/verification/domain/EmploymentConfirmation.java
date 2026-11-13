package verification.domain;

import java.time.Instant;
import verification.domain.exception.EmploymentNotConfirmedException;
import verification.domain.exception.WorkPassNotConfirmedException;

/**
 * The provider's confirmation that a verified, named person is lawfully employed by or engaged with
 * them and allowed to work in Malaysia. It is recorded with its time and the version of the terms.
 */
public record EmploymentConfirmation(String termsVersion, boolean workPassConfirmed, Instant confirmedAt) {

    public static final String MALAYSIA = "MY";

    static EmploymentConfirmation of(String documentCountry, boolean lawfullyEmployed, boolean holdsValidWorkPass,
                                     String termsVersion, Instant now) {
        if (!lawfullyEmployed) {
            throw new EmploymentNotConfirmedException();
        }
        boolean foreignDocument = !MALAYSIA.equals(documentCountry);
        if (foreignDocument && !holdsValidWorkPass) {
            throw new WorkPassNotConfirmedException();
        }
        return new EmploymentConfirmation(termsVersion, foreignDocument, now);
    }
}
