package account.usecase;

import account.domain.BusinessRegistration;
import account.domain.KybResult;
import account.domain.ProviderAccount;
import account.domain.VerificationOutcome;
import account.usecase.dto.RecordKybResultRequest;
import account.usecase.port.in.KycBusinessResultRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.ProviderReverifiedIE;
import ie.events.ProviderVerificationRejectedIE;
import ie.events.ProviderVerifiedIE;
import ie.events.RecheckFailedIE;
import ie.port.AccountIE;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Records the KYC provider's KYB or re-verification result for a business, and publishes one event for the
 * outcome the domain returns (R1). A result that changes nothing is not saved or told. A pass without an
 * SSM number, registered name or verified-until date is refused by the domain (R9).
 */
public final class KycRecordBusinessResult implements KycBusinessResultRecorder {

    private static final String THE_BUSINESS = "";

    private final ProviderAccountRepository providers;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordBusinessResult(ProviderAccountRepository providers, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordKybResultRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());
        Instant now = clock.instant();

        VerificationOutcome outcome = provider.recordKybResult(result(request), now);

        Optional<AccountIE> told = event(outcome, provider, now);
        if (told.isPresent()) {
            providers.save(provider);
            events.publish(told.get());
        }
    }

    private static KybResult result(RecordKybResultRequest request) {
        if (!request.verified()) {
            return KybResult.rejected();
        }
        return KybResult.verified(new BusinessRegistration(request.ssmNumber(), request.registeredName()), request.verifiedUntil());
    }

    private static Optional<AccountIE> event(VerificationOutcome outcome, ProviderAccount provider, Instant now) {
        return Optional.ofNullable(switch (outcome) {
            case VERIFIED -> new ProviderVerifiedIE(
                    provider.id(), provider.businessRegistration().orElseThrow().ssmNumber(), provider.verifiedUntil().orElseThrow(), now);
            case REJECTED -> new ProviderVerificationRejectedIE(provider.id(), now);
            case REVERIFIED -> new ProviderReverifiedIE(provider.id(), provider.verifiedUntil().orElseThrow(), now);
            case RECHECK_FAILED -> new RecheckFailedIE(provider.id(), THE_BUSINESS, now);
            case UNCHANGED -> null;
        });
    }
}
