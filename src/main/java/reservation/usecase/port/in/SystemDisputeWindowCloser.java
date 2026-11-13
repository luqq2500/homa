package reservation.usecase.port.in;

import reservation.usecase.dto.CompleteAfterDisputeWindowResponse;

/** Reservations whose dispute window ended with no report are completed and their payment released. */
public interface SystemDisputeWindowCloser {

    CompleteAfterDisputeWindowResponse completeDue();
}
