package reservation.domain.exception;

/** Completion needs photo or video evidence. */
public class EvidenceRequiredException extends ReservationRuleViolationException {

    public EvidenceRequiredException() {
        super("Completion needs photo or video evidence");
    }
}
