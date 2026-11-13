package reservation.usecase.port.in;

import reservation.usecase.dto.StartEnquiryRequest;
import reservation.usecase.dto.StartEnquiryResponse;

/** A customer picks the details on the provider page and taps Chat. */
public interface CustomerEnquiryStarter {

    StartEnquiryResponse start(StartEnquiryRequest request);
}
