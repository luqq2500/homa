package verification.usecase.port.in;

import verification.usecase.dto.StartTeamMemberReverificationRequest;
import verification.usecase.dto.StartTeamMemberReverificationResponse;

/** A provider starts a team member's re-verification on the spot. */
public interface ProviderTeamMemberReverificationStarter {

    StartTeamMemberReverificationResponse start(StartTeamMemberReverificationRequest request);
}
