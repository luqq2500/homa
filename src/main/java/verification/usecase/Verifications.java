package verification.usecase;

import ie.events.BusinessVerifiedIE;
import ie.events.TeamMemberJoinedIE;
import ie.events.TeamMemberLeftIE;
import ie.events.TeamMemberRestoredIE;
import java.time.Instant;
import verification.domain.Business;
import verification.domain.Role;
import verification.domain.TeamMember;
import verification.usecase.exception.BusinessNotFoundException;
import verification.usecase.exception.TeamMemberNotFoundException;
import verification.usecase.exception.UnknownRoleException;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.TeamMemberRepository;

/** What the verification usecases share: lookups, reading what was typed, and building events. */
final class Verifications {

    static final String LAPSED = "LAPSED";
    static final String REMOVED = "REMOVED";

    private Verifications() {
    }

    static Business business(BusinessRepository businesses, String businessId) {
        return businesses.find(businessId).orElseThrow(() -> new BusinessNotFoundException(businessId));
    }

    static TeamMember member(TeamMemberRepository members, String memberId) {
        return members.find(memberId).orElseThrow(() -> new TeamMemberNotFoundException(memberId));
    }

    /** The person, as long as they are with this business: a provider only reaches their own people. */
    static TeamMember member(TeamMemberRepository members, String businessId, String memberId) {
        TeamMember member = member(members, memberId);
        if (!member.businessId().equals(businessId)) {
            throw new TeamMemberNotFoundException(memberId);
        }
        return member;
    }

    static Role role(String name) {
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownRoleException(name);
        }
    }

    static BusinessVerifiedIE verified(Business business, Instant now) {
        return new BusinessVerifiedIE(business.id(), business.ssmNumber(), business.registeredName(),
                business.verifiedUntil(), business.lastVerifiedAt(), now);
    }

    static TeamMemberJoinedIE joined(TeamMember member, Instant now) {
        return new TeamMemberJoinedIE(member.id(), member.businessId(), member.role().name(),
                member.registeredName(), member.profilePhoto(), member.lastVerifiedAt(), now);
    }

    static TeamMemberRestoredIE restored(TeamMember member, Instant now) {
        return new TeamMemberRestoredIE(member.id(), member.businessId(), member.role().name(),
                member.registeredName(), member.profilePhoto(), member.lastVerifiedAt(), now);
    }

    static TeamMemberLeftIE left(TeamMember member, String reason, Instant now) {
        return new TeamMemberLeftIE(member.id(), member.businessId(), reason, now);
    }
}
