package offering.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The provider's declaration of their liability insurance: the insurer and the date it is valid until.
 * Homa doesn't check the policy; the provider is responsible for the declaration.
 */
public record InsuranceDeclaration(String insurer, Instant validUntil) {

    public InsuranceDeclaration {
        insurer = OfferingText.require(insurer, "An insurer");
        Objects.requireNonNull(validUntil);
    }

    public static InsuranceDeclaration of(String insurer, Instant validUntil) {
        return new InsuranceDeclaration(insurer, validUntil);
    }
}
