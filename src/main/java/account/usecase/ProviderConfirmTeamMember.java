package account.usecase;

import account.domain.EmploymentConfirmation;
import account.domain.MemberVerification;
import account.domain.TeamMember;
import account.usecase.dto.ConfirmTeamMemberRequest;
import account.usecase.dto.ConfirmTeamMemberResponse;
import account.usecase.exception.MemberVerificationNotFoundException;
import account.usecase.port.in.ProviderTeamMemberConfirmer;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.MemberVerificationRepository;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.TeamMemberAddedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * The provider confirms that a verified, named person is lawfully employed by or engaged with them, and the
 * person joins the team with their registered name (R7). A business confirms only its own people.
 */
public final class ProviderConfirmTeamMember implements ProviderTeamMemberConfirmer {

    private final MemberVerificationRepository verifications;
    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public ProviderConfirmTeamMember(
            MemberVerificationRepository verifications, TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.verifications = verifications;
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public ConfirmTeamMemberResponse confirm(ConfirmTeamMemberRequest request) {
        MemberVerification verification = Accounts.memberVerification(verifications, request.verificationId());
        if (!verification.providerId().equals(request.providerId())) {
            throw new MemberVerificationNotFoundException(request.verificationId());
        }
        Instant now = clock.instant();
        EmploymentConfirmation confirmation =
                request.lawfullyEmployed() ? EmploymentConfirmation.given(request.termsVersion(), now) : null;

        verification.confirmEmployment(confirmation);
        TeamMember member = TeamMember.add(verification, now);

        verifications.save(verification);
        members.save(member);
        events.publish(new TeamMemberAddedIE(
                member.id(), member.providerId(), verification.id(), member.role().name(), member.registeredName(),
                member.employmentConfirmation().termsVersion(), member.employmentConfirmation().givenAt(), now));
        return new ConfirmTeamMemberResponse(member.id());
    }
}
