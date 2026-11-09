package policy.domain.exception;

/** A country is an ISO 3166 two-letter code, such as MY. */
public class InvalidCountryCodeException extends PolicyRuleViolationException {

    public InvalidCountryCodeException(String code) {
        super("Not an ISO 3166 country code: " + code);
    }
}
