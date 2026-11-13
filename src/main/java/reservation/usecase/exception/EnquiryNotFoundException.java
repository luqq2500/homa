package reservation.usecase.exception;

public class EnquiryNotFoundException extends RuntimeException {

    public EnquiryNotFoundException(String enquiryId) {
        super("No enquiry " + enquiryId);
    }
}
