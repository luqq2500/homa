package reservation.usecase.port.in;

import reservation.usecase.dto.ExpireRequestsResponse;

/** Requests the provider did not answer in time expire. */
public interface SystemRequestExpirer {

    ExpireRequestsResponse expireDue();
}
