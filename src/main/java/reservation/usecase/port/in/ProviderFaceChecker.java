package reservation.usecase.port.in;

import reservation.usecase.dto.FaceCheckRequest;
import reservation.usecase.dto.FaceCheckResponse;

/** After check-in, each other named person takes the face check before working. */
public interface ProviderFaceChecker {

    FaceCheckResponse check(FaceCheckRequest request);
}
