package catalogue.usecase.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("No job " + jobId);
    }
}
