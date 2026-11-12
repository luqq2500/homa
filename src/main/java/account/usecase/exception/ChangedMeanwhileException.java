package account.usecase.exception;

/** Something changed after it was read, so this change is refused rather than overwriting the other. Thrown by a repository's save. */
public class ChangedMeanwhileException extends AccountUsecaseException {

    public ChangedMeanwhileException(String what) {
        super(what + " changed meanwhile; try again on what is there now");
    }
}
