package reservation.usecase.port.in;

import reservation.usecase.dto.AcceptRequestRequest;
import reservation.usecase.dto.AcceptRequestResponse;

/** A provider accepts a request by the response deadline. */
public interface ProviderRequestAccepter {

    AcceptRequestResponse accept(AcceptRequestRequest request);
}
