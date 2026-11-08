package payment.domain.exception;

/** Homa records why it waived a fine. */
public class WaiverReasonRequiredException extends PaymentRuleViolationException {

    public WaiverReasonRequiredException() {
        super("A waiver needs a reason");
    }
}
