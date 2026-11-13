package offering.domain;

import java.time.Instant;
import offering.domain.exception.InsuranceIncompleteException;

/**
 * A provider's declared liability insurance. Homa does not check the policy; the provider is
 * responsible for the declaration. The Insured badge shows until the date it is valid until.
 */
public record InsuranceDeclaration(String insurer, Instant validUntil) {

    public InsuranceDeclaration {
        if (insurer == null || insurer.isBlank() || validUntil == null) {
            throw new InsuranceIncompleteException();
        }
        insurer = insurer.strip();
    }

    public boolean isValidAt(Instant now) {
        return !now.isAfter(validUntil);
    }
}
