package reservation.domain.exception;

public class EnquiryClosedException extends RuntimeException {

    public EnquiryClosedException(String enquiryId) {
        super("The enquiry " + enquiryId + " is closed");
    }
}
