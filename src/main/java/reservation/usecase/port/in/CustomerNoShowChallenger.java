package reservation.usecase.port.in;

import reservation.usecase.dto.ChallengeNoShowRequest;
import reservation.usecase.dto.ChallengeNoShowResponse;

/** A customer reports a wrongful no-show within the no-show report window. */
public interface CustomerNoShowChallenger {

    ChallengeNoShowResponse challenge(ChallengeNoShowRequest request);
}
