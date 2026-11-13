package reservation.usecase.port.in;

import reservation.usecase.dto.CheckInRequest;
import reservation.usecase.dto.CheckInResponse;

/** A provider checks in at the door: the device's location, and a named person's live selfie. */
public interface ProviderCheckerIn {

    CheckInResponse checkIn(CheckInRequest request);
}
