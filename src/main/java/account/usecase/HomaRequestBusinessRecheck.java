package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.HomaRequestBusinessRecheckRequest;
import account.usecase.port.in.HomaBusinessRecheckRequester;
import account.usecase.port.out.AdminAuditLog;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.ProviderAccountRepository;
import java.time.Clock;

/**
 * Asks the KYC provider for an early re-verification of a business, with a reason, and records it.
 * Nothing changes in Homa until the KYC provider reports a pass or a lapse.
 */
public final class HomaRequestBusinessRecheck implements HomaBusinessRecheckRequester {

    private final ProviderAccountRepository providers;
    private final IdentityVerificationService kyc;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRequestBusinessRecheck(ProviderAccountRepository providers, IdentityVerificationService kyc, AdminAuditLog auditLog, Clock clock) {
        this.providers = providers;
        this.kyc = kyc;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public void request(HomaRequestBusinessRecheckRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "ask for an early re-check");
        ProviderAccount provider = Accounts.provider(providers, request.providerId());

        kyc.requestBusinessRecheck(provider.id(), reason);

        admin.record(founder, "REQUEST_BUSINESS_RECHECK", provider.id(), reason, clock.instant());
    }
}
