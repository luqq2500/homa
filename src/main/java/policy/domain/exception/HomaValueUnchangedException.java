package policy.domain.exception;

import policy.domain.HomaValue;

public class HomaValueUnchangedException extends RuntimeException {

    public HomaValueUnchangedException(HomaValue value) {
        super(value + " already has this setting");
    }
}
