package reservation.usecase.port.in;

import reservation.usecase.dto.CancelByProviderRequest;
import reservation.usecase.dto.CancelByProviderResponse;

/** A provider cancels a reserved booking; the customer is refunded in full and the provider may be fined. */
public interface ProviderReservationCanceller {

    CancelByProviderResponse cancel(CancelByProviderRequest request);
}
