package account.usecase.port.in;

import account.usecase.dto.ProviderViewAccountRequest;
import account.usecase.dto.ProviderViewAccountResponse;

/** A business views its account: its verification and whether it is ready to go live. */
public interface ProviderAccountViewer {

    ProviderViewAccountResponse view(ProviderViewAccountRequest request);
}
