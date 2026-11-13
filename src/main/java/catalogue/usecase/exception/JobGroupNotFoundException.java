package catalogue.usecase.exception;

public class JobGroupNotFoundException extends RuntimeException {

    public JobGroupNotFoundException(String groupId) {
        super("No job group " + groupId);
    }
}
