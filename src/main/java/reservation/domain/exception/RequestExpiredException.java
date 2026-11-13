package reservation.domain.exception;

public class RequestExpiredException extends RuntimeException {

    public RequestExpiredException() {
        super("The request has expired");
    }
}
