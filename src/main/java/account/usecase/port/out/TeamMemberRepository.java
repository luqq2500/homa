package account.usecase.port.out;

import account.domain.TeamMember;
import account.usecase.exception.ChangedMeanwhileException;
import java.util.List;
import java.util.Optional;

/** Keeps the team members of every business.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found. Nothing is overwritten; the caller tries again on what is there now.
 */
public interface TeamMemberRepository {

    Optional<TeamMember> find(String memberId);

    /** The business's team members, lapsed and removed ones included, in the order they were added. */
    List<TeamMember> findFor(String providerId);

    void save(TeamMember member);
}
