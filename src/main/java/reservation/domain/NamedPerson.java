package reservation.domain;

/** A team member named for a job, in the role they were named in. */
public record NamedPerson(String memberId, TeamRole role) {
}
