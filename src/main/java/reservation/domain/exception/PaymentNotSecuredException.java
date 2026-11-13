package reservation.domain.exception;

public class PaymentNotSecuredException extends RuntimeException {

    public PaymentNotSecuredException() {
        super("The request is not paid or authorised yet");
    }
}
