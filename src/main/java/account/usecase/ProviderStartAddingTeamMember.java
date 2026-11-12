package account.usecase;

import account.domain.EmploymentConfirmation;
import account.domain.MemberVerification;
import account.domain.Photo;
import account.domain.ProviderAccount;
import account.domain.TeamRole;
import account.usecase.dto.StartAddingTeamMemberRequest;
import account.usecase.dto.StartAddingTeamMemberResponse;
import account.usecase.exception.UnknownTeamRoleException;
import account.usecase.port.in.ProviderTeamMemberAdder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.MemberVerificationRepository;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.MemberVerificationStartedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;

/**
 * Starts adding a helper or a supervisor from the add form, with the lawful-employment confirmation, and
 * opens the KYC provider's on-the-spot check on the provider's phone. The person is waiting for
 * verification, not on the team, until the KYC provider reports a pass. The verification is saved before
 * the check opens, so the KYC provider's result always finds it.
 */
public final class ProviderStartAddingTeamMember implements ProviderTeamMemberAdder {

    private final ProviderAccountRepository providers;
    private final MemberVerificationRepository verifications;
    private final IdentityVerificationService kyc;
    private final AccountIEPublisher events;
    private final Clock clock;

    public ProviderStartAddingTeamMember(ProviderAccountRepository providers, MemberVerificationRepository verifications, IdentityVerificationService kyc, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.verifications = verifications;
        this.kyc = kyc;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public StartAddingTeamMemberResponse start(StartAddingTeamMemberRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());
        TeamRole role = role(request.role());
        Instant now = clock.instant();
        EmploymentConfirmation confirmation =
                request.lawfullyEmployed() ? EmploymentConfirmation.given(request.termsVersion(), now) : null;

        MemberVerification verification = MemberVerification.start(provider, role, Photo.of(request.photo()), confirmation, now);

        verifications.save(verification);
        String checkUrl = kyc.startMemberCheck(verification.id());
        events.publish(new MemberVerificationStartedIE(verification.id(), provider.id(), role.name(), now));
        return new StartAddingTeamMemberResponse(verification.id(), checkUrl);
    }

    private static TeamRole role(String role) {
        return Arrays.stream(TeamRole.values())
                .filter(known -> known.name().equals(role))
                .findFirst()
                .orElseThrow(() -> new UnknownTeamRoleException(role));
    }
}
