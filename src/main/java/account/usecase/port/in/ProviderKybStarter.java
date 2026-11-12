package account.usecase.port.in;

import account.usecase.dto.StartKybRequest;
import account.usecase.dto.StartKybResponse;

/** A business opens the KYC provider's KYB pages, to verify or to re-verify. */
public interface ProviderKybStarter {

    StartKybResponse start(StartKybRequest request);
}
