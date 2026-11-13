package offering.domain.exception;

public class InsuranceIncompleteException extends RuntimeException {

    public InsuranceIncompleteException() {
        super("An insurance declaration names the insurer and the date it is valid until");
    }
}
