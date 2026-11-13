package reservation.usecase.port.in;

import reservation.usecase.dto.SubmitRequestRequest;
import reservation.usecase.dto.SubmitRequestResponse;

/** A customer taps Reserve: directly, or from an enquiry's chat. The payment method is CARD or FPX. */
public interface CustomerRequestSubmitter {

    SubmitRequestResponse submit(SubmitRequestRequest request);
}
