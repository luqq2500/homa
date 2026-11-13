package reservation.usecase.port.in;

import reservation.usecase.dto.EditRequestRequest;
import reservation.usecase.dto.EditRequestResponse;

/** A customer edits the jobs, date and time, duration or helpers of a request until the provider accepts. */
public interface CustomerRequestEditor {

    EditRequestResponse edit(EditRequestRequest request);
}
