package policy.domain.exception;

public class HomaValueRuleBrokenException extends RuntimeException {

    public HomaValueRuleBrokenException(String rule) {
        super(rule);
    }
}
