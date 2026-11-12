package account.usecase.port.in;

import account.usecase.dto.ProviderRemoveTeamMemberRequest;

/** A business removes one of its team members. */
public interface ProviderTeamMemberRemover {

    void remove(ProviderRemoveTeamMemberRequest request);
}
