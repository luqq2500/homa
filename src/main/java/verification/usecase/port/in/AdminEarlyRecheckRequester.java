package verification.usecase.port.in;

import verification.usecase.dto.RequestEarlyRecheckRequest;
import verification.usecase.dto.RequestEarlyRecheckResponse;

/** An admin asks the KYC provider for an early re-check of a business or a team member, with a reason. */
public interface AdminEarlyRecheckRequester {

    RequestEarlyRecheckResponse request(RequestEarlyRecheckRequest request);
}
