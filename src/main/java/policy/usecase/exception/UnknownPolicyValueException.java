package policy.usecase.exception;

/** No Homa-set value has this name. */
public class UnknownPolicyValueException extends PolicyUsecaseException {

    public UnknownPolicyValueException(String value) {
        super("There is no Homa-set value named " + value);
    }
}
