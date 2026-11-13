package reservation.usecase.port.in;

import reservation.usecase.dto.DeclineRequestRequest;
import reservation.usecase.dto.DeclineRequestResponse;

/** A provider declines a request. */
public interface ProviderRequestDecliner {

    DeclineRequestResponse decline(DeclineRequestRequest request);
}
