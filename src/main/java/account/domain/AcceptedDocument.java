package account.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * One entry on the accepted documents list.
 *
 * @param issuingCountry the country that must have issued it; empty for any country
 */
public record AcceptedDocument(DocumentHolder holder, DocumentType type, Optional<Country> issuingCountry) {

    public AcceptedDocument {
        Objects.requireNonNull(holder);
        Objects.requireNonNull(type);
        Objects.requireNonNull(issuingCountry);
    }

    boolean accepts(DocumentHolder who, DocumentType documentType, Country country) {
        return holder == who && type == documentType && issuingCountry.map(country::equals).orElse(true);
    }
}
