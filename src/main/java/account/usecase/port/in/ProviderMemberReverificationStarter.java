package account.usecase.port.in;

import account.usecase.dto.StartMemberReverificationRequest;
import account.usecase.dto.StartMemberReverificationResponse;

/** A business opens a team member's re-verification, with the person present. */
public interface ProviderMemberReverificationStarter {

    StartMemberReverificationResponse start(StartMemberReverificationRequest request);
}
