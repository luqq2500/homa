package verification.domain.exception;

public class EmploymentNotConfirmedException extends RuntimeException {

    public EmploymentNotConfirmedException() {
        super("The provider confirms the person is lawfully employed or engaged and allowed to work in Malaysia");
    }
}
