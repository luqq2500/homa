package reservation.domain.exception;

public class PersonCannotWorkException extends RuntimeException {

    public PersonCannotWorkException(String memberId) {
        super("The person " + memberId + " is not on this job's team or failed the face check");
    }
}
