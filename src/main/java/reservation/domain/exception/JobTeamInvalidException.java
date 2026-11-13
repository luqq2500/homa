package reservation.domain.exception;

public class JobTeamInvalidException extends RuntimeException {

    public JobTeamInvalidException(String problem) {
        super(problem);
    }
}
