package verification.usecase.port.in;

import verification.usecase.dto.StartBusinessCheckRequest;
import verification.usecase.dto.StartBusinessCheckResponse;

/** A provider starts the KYB, or a re-verification of the business, on the KYC provider's pages. */
public interface ProviderBusinessCheckStarter {

    StartBusinessCheckResponse start(StartBusinessCheckRequest request);
}
