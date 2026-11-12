package account.usecase;

import account.domain.CustomerAccount;
import account.domain.MemberVerification;
import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.domain.TeamMember;
import account.domain.TeamStatus;
import account.usecase.exception.CustomerNotFoundException;
import account.usecase.exception.MemberVerificationNotFoundException;
import account.usecase.exception.NotYourTeamMemberException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.exception.ProviderNotFoundException;
import account.usecase.exception.TeamMemberNotFoundException;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.MemberVerificationRepository;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.ProviderAccountRepository;
import account.usecase.port.out.TeamMemberRepository;

/** Loading what an account usecase works on, or saying it is not there. */
final class Accounts {

    private Accounts() {
    }

    static PhoneVerification verification(PhoneVerificationRepository verifications, String verificationId) {
        return verifications.find(verificationId).orElseThrow(() -> new PhoneVerificationNotFoundException(verificationId));
    }

    static CustomerAccount customer(CustomerAccountRepository customers, String customerId) {
        return customers.find(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    static ProviderAccount provider(ProviderAccountRepository providers, String providerId) {
        return providers.find(providerId).orElseThrow(() -> new ProviderNotFoundException(providerId));
    }

    static MemberVerification memberVerification(MemberVerificationRepository verifications, String verificationId) {
        return verifications.find(verificationId).orElseThrow(() -> new MemberVerificationNotFoundException(verificationId));
    }

    static TeamMember member(TeamMemberRepository members, String memberId) {
        return members.find(memberId).orElseThrow(() -> new TeamMemberNotFoundException(memberId));
    }

    /** A team member of {@code providerId} who was not removed: a business acts only on its own team. */
    static TeamMember memberOf(TeamMemberRepository members, String memberId, String providerId) {
        TeamMember member = member(members, memberId);
        if (!member.providerId().equals(providerId)) {
            throw new NotYourTeamMemberException(memberId, providerId);
        }
        if (member.teamStatus() == TeamStatus.REMOVED) {
            throw new TeamMemberNotFoundException(memberId);
        }
        return member;
    }
}
