package reservation.usecase.port.in;

import reservation.usecase.dto.ViewProviderReservationRequest;
import reservation.usecase.dto.ViewProviderReservationResponse;

/** A provider sees a request or reservation of theirs once its payment is secured; access notes show from acceptance. */
public interface ProviderReservationViewer {

    ViewProviderReservationResponse view(ViewProviderReservationRequest request);
}
