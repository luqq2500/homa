package reservation.domain.exception;

public class JobTeamNotNamedException extends RuntimeException {

    public JobTeamNotNamedException() {
        super("Check-in is enabled only once the team is named");
    }
}
