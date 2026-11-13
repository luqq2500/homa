package reservation.usecase.port.in;

import reservation.usecase.dto.CancelByCustomerRequest;
import reservation.usecase.dto.CancelByCustomerResponse;

/** A customer cancels a request or a reservation. */
public interface CustomerReservationCanceller {

    CancelByCustomerResponse cancel(CancelByCustomerRequest request);
}
