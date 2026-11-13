package account.usecase.exception;

public class UnknownLanguageException extends RuntimeException {

    public UnknownLanguageException(String language) {
        super("Homa is not in the language " + language);
    }
}
