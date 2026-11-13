package reservation.usecase.port.in;

import reservation.usecase.dto.CancelUndecidedFlaggedResponse;

/** Flagged reservations nobody decided by the booked start are cancelled with a full refund. */
public interface SystemUndecidedFlagCanceller {

    CancelUndecidedFlaggedResponse cancelDue();
}
