package account.usecase.port.in;

import account.usecase.dto.ProviderViewTeamRequest;
import account.usecase.dto.ProviderViewTeamResponse;

/** A business views its team and anyone waiting for verification. */
public interface ProviderTeamViewer {

    ProviderViewTeamResponse view(ProviderViewTeamRequest request);
}
