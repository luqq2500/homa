package reservation.usecase.port.in;

import reservation.usecase.dto.CloseEnquiriesResponse;

/** Enquiries that were never requested close when their date passes. */
public interface SystemEnquiryCloser {

    CloseEnquiriesResponse closePassed();
}
