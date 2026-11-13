package reservation.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import reservation.domain.exception.JobTeamInvalidException;

/**
 * Who is coming: the helpers named for a reservation, matching the number booked, and at least one
 * supervisor when the booking includes one. A named person keeps the role they were named in.
 */
public record JobTeam(List<NamedPerson> people) {

    public JobTeam {
        people = List.copyOf(people);
    }

    /** Names the team from the provider's current team members. */
    public static JobTeam name(Set<String> memberIds, TeamRoster roster, int helpersBooked,
                               boolean supervisorIncluded) {
        List<NamedPerson> people = memberIds.stream().sorted().map(memberId -> named(memberId, roster)).toList();
        JobTeam team = new JobTeam(people);
        if (team.count(TeamRole.HELPER) != helpersBooked) {
            throw new JobTeamInvalidException("The helpers named match the " + helpersBooked + " helpers booked");
        }
        if (!team.isCompleteFor(helpersBooked, supervisorIncluded)) {
            throw new JobTeamInvalidException("This booking includes a supervisor, so at least one is named");
        }
        return team;
    }

    /** Whether everyone who must come is named: the helpers booked, and a supervisor when one is included. */
    public boolean isCompleteFor(int helpersBooked, boolean supervisorIncluded) {
        boolean supervisorNamed = !supervisorIncluded || count(TeamRole.SUPERVISOR) >= 1;
        return count(TeamRole.HELPER) == helpersBooked && supervisorNamed;
    }

    private static NamedPerson named(String memberId, TeamRoster roster) {
        TeamRole role = roster.roleOf(memberId)
                .orElseThrow(() -> new JobTeamInvalidException("Only people on the team can work: " + memberId));
        return new NamedPerson(memberId, role);
    }

    public boolean includes(String memberId) {
        return people.stream().anyMatch(person -> person.memberId().equals(memberId));
    }

    /** The team without a person who lapsed or was removed. */
    public JobTeam without(String memberId) {
        return new JobTeam(people.stream().filter(person -> !person.memberId().equals(memberId)).toList());
    }

    public Set<String> memberIds() {
        return people.stream().map(NamedPerson::memberId).collect(Collectors.toUnmodifiableSet());
    }

    private long count(TeamRole role) {
        return people.stream().filter(person -> person.role() == role).count();
    }
}
