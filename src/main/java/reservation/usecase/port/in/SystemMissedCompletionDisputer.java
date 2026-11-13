package reservation.usecase.port.in;

import reservation.usecase.dto.DisputeMissedCompletionsResponse;

/** Reservations whose provider missed the completion deadline move to Disputed. */
public interface SystemMissedCompletionDisputer {

    DisputeMissedCompletionsResponse disputeDue();
}
