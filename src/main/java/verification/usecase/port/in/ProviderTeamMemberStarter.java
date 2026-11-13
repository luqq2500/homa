package verification.usecase.port.in;

import verification.usecase.dto.StartTeamMemberRequest;
import verification.usecase.dto.StartTeamMemberResponse;

/** A provider starts adding a person: their role and a profile photo taken with the camera. */
public interface ProviderTeamMemberStarter {

    StartTeamMemberResponse start(StartTeamMemberRequest request);
}
