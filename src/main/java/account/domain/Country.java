package account.domain;

import account.domain.exception.InvalidCountryException;
import java.util.Locale;
import java.util.Set;

/** The country that issued an identity document, as an ISO 3166 two-letter code such as {@code MY}. */
public record Country(String code) {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public static final Country MALAYSIA = new Country("MY");

    public Country {
        if (code == null || !ISO_COUNTRIES.contains(code)) {
            throw new InvalidCountryException(code);
        }
    }

    public static Country of(String code) {
        return new Country(code);
    }
}
