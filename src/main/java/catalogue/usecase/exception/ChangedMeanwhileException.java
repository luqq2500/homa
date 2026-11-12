package catalogue.usecase.exception;

/**
 * Something changed after it was read, for example because another founder saved first, so this change is
 * refused rather than overwriting theirs. Thrown by a repository's save; the founder tries again.
 */
public class ChangedMeanwhileException extends CatalogueUsecaseException {

    public ChangedMeanwhileException(String what) {
        super(what + " changed meanwhile; try again on what is there now");
    }
}
