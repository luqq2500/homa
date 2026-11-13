package account.usecase.port.in;

import account.usecase.dto.ChangeAccessNotesRequest;
import account.usecase.dto.ChangeAccessNotesResponse;

/** A customer changes the access notes kept on their account for the team. */
public interface CustomerAccessNotesChanger {

    ChangeAccessNotesResponse change(ChangeAccessNotesRequest request);
}
