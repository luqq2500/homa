package reservation.usecase.port.in;

import reservation.usecase.dto.ChangeAccessNotesRequest;
import reservation.usecase.dto.ChangeAccessNotesResponse;

/** A customer changes a reservation's access notes until the job is completed. */
public interface CustomerAccessNotesChanger {

    ChangeAccessNotesResponse change(ChangeAccessNotesRequest request);
}
