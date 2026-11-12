package account.usecase.dto;

import java.time.Instant;
import java.util.List;

/**
 * A business's team, in the order the members were added, without removed members; and the people still
 * waiting for verification, oldest first.
 */
public record ProviderViewTeamResponse(List<Member> members, List<Waiting> waiting) {

    public ProviderViewTeamResponse {
        members = List.copyOf(members);
        waiting = List.copyOf(waiting);
    }

    /**
     * One team member.
     *
     * @param status {@code MEMBER}, or {@code LAPSED} until they pass a re-verification
     * @param verifiedUntil when they are next due for re-verification
     */
    public record Member(
            String memberId, String registeredName, String role, String status, Instant verifiedUntil, Instant lastVerifiedAt) {
    }

    /** One person waiting for the KYC provider's result: not on the team, and not shown to customers. */
    public record Waiting(String verificationId, String role, Instant startedAt) {
    }
}
