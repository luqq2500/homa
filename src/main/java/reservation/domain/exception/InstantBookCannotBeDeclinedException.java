package reservation.domain.exception;

public class InstantBookCannotBeDeclinedException extends RuntimeException {

    public InstantBookCannotBeDeclinedException() {
        super("An instant-book request cannot be declined");
    }
}
