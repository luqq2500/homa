package policy.domain;

import policy.domain.exception.InvalidCountryCodeException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One entry on the accepted documents list: who may use which document type, issued by which country.
 *
 * @param issuingCountry an ISO 3166 two-letter country code, such as {@code MY}; empty for any country
 */
public record AcceptedDocument(DocumentHolder holder, DocumentType type, Optional<String> issuingCountry) {

    /** Malaysia, the only country a provider and an independent provider's team members may use. */
    public static final String MALAYSIA = "MY";

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public AcceptedDocument {
        Objects.requireNonNull(holder);
        Objects.requireNonNull(type);
        Objects.requireNonNull(issuingCountry);
        issuingCountry.filter(code -> !ISO_COUNTRIES.contains(code)).ifPresent(code -> {
            throw new InvalidCountryCodeException(code);
        });
    }

    public static AcceptedDocument issuedBy(DocumentHolder holder, DocumentType type, String country) {
        return new AcceptedDocument(holder, type, Optional.of(country));
    }

    public static AcceptedDocument fromAnyCountry(DocumentHolder holder, DocumentType type) {
        return new AcceptedDocument(holder, type, Optional.empty());
    }

    boolean isMalaysianOnly() {
        return issuingCountry.filter(MALAYSIA::equals).isPresent();
    }
}
