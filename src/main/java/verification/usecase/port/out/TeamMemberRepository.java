package verification.usecase.port.out;

import java.util.List;
import java.util.Optional;
import verification.domain.TeamMember;

public interface TeamMemberRepository {

    void save(TeamMember teamMember);

    Optional<TeamMember> find(String id);

    List<TeamMember> all();

    List<TeamMember> ofBusiness(String businessId);
}
