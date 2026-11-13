package catalogue.domain.exception;

public class JobRetiredException extends RuntimeException {

    public JobRetiredException(String jobId) {
        super("The job " + jobId + " is retired");
    }
}
