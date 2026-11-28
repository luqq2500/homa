package com.homa.provider.usecase;

import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.provider.domain.TeamMember;
import java.time.Instant;
import java.time.LocalDate;

/** Builds the events about a team member from the member as saved, field by field. */
final class TeamMemberEvents {

    static final String LOCAL = "LOCAL";
    static final String FOREIGN_WORKER = "FOREIGN_WORKER";
    static final String DEACTIVATED = "DEACTIVATED";
    static final String REMOVED = "REMOVED";

    private TeamMemberEvents() {
    }

    /** The member is active: just added (HLP-1 to HLP-4, PRV-6) or reactivated (HLP-5). */
    static TeamMemberActivatedIE activated(String eventId, TeamMember member, String displayName, Instant at) {
        return new TeamMemberActivatedIE(
                eventId,
                member.id().value(),
                member.personId().value(),
                member.providerId().value(),
                displayName,
                member.role().name(),
                eligibilityOf(member),
                member.hasVerifiedPhoto(),
                member.rosterableUntil().map(LocalDate::toString).orElse(null),
                at);
    }

    /** The member stopped being active because the provider deactivated or removed them; {@code reason} says which. */
    static TeamMemberDeactivatedIE deactivated(String eventId, TeamMember member, String reason, Instant at) {
        return new TeamMemberDeactivatedIE(
                eventId, member.id().value(), member.personId().value(), member.providerId().value(), reason, at);
    }

    static String eligibilityOf(TeamMember member) {
        return member.eligibility().isLocal() ? LOCAL : FOREIGN_WORKER;
    }
}
