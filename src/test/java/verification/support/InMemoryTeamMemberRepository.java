package verification.support;

import java.util.List;
import java.util.Optional;
import support.InMemoryStore;
import verification.domain.TeamMember;
import verification.usecase.port.out.TeamMemberRepository;

public final class InMemoryTeamMemberRepository extends InMemoryStore<TeamMember> implements TeamMemberRepository {

    public InMemoryTeamMemberRepository() {
        super(TeamMember::id);
    }

    @Override
    public List<TeamMember> ofBusiness(String businessId) {
        return all().stream().filter(member -> member.businessId().equals(businessId)).toList();
    }
}
