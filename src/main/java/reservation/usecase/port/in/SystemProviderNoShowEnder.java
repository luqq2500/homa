package reservation.usecase.port.in;

import reservation.usecase.dto.EndProviderNoShowsResponse;

/** Reservations with no check-in by the end of the check-in period end as provider no-shows. */
public interface SystemProviderNoShowEnder {

    EndProviderNoShowsResponse endDue();
}
