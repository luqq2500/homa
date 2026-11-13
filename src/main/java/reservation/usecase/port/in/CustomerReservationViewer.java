package reservation.usecase.port.in;

import reservation.usecase.dto.ViewCustomerReservationRequest;
import reservation.usecase.dto.ViewCustomerReservationResponse;

/** A customer sees a request or reservation of theirs. */
public interface CustomerReservationViewer {

    ViewCustomerReservationResponse view(ViewCustomerReservationRequest request);
}
