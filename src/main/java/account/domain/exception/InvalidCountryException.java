package account.domain.exception;

/** A country is an ISO 3166 two-letter code, such as MY. */
public class InvalidCountryException extends AccountRuleViolationException {

    public InvalidCountryException(String code) {
        super("Not an ISO 3166 country code: " + code);
    }
}
