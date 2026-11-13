package policy.domain.exception;

import policy.domain.HomaValue;

public class HomaValueOutOfRangeException extends RuntimeException {

    public HomaValueOutOfRangeException(HomaValue value, Long amount) {
        super(value + " cannot be " + amount);
    }
}
