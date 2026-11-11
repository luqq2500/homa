package reservation.domain.exception;

/** Each named person has one face check result from the KYC provider: passed, or failed and unable to work. */
public class FaceCheckAlreadyRecordedException extends ReservationRuleViolationException {

    public FaceCheckAlreadyRecordedException(String memberId) {
        super("Team member " + memberId + " already has a face check result");
    }
}
