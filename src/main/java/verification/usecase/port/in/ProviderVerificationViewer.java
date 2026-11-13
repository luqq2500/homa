package verification.usecase.port.in;

import verification.usecase.dto.ViewVerificationRequest;
import verification.usecase.dto.ViewVerificationResponse;

/** A provider sees when the business and each person are next due for re-verification. */
public interface ProviderVerificationViewer {

    ViewVerificationResponse view(ViewVerificationRequest request);
}
