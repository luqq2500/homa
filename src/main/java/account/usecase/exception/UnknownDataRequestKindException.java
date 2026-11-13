package account.usecase.exception;

public class UnknownDataRequestKindException extends RuntimeException {

    public UnknownDataRequestKindException(String kind) {
        super("No kind of data request called " + kind);
    }
}
