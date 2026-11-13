package account.usecase.port.in;

import account.usecase.dto.HandleDataRequestRequest;
import account.usecase.dto.HandleDataRequestResponse;

/** An admin handles a data request and records it. */
public interface AdminDataRequestHandler {

    HandleDataRequestResponse handle(HandleDataRequestRequest request);
}
