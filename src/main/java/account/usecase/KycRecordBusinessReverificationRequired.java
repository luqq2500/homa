package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.RecordBusinessReverificationRequiredRequest;
import account.usecase.port.in.KycBusinessReverificationRequirer;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.ReverificationRequiredIE;
import java.time.Clock;

/**
 * The KYC provider's monitoring asks a business to re-verify by a date, for example after a change of
 * director. It keeps working, and its verified-until date moves to the deadline if that is sooner.
 */
public final class KycRecordBusinessReverificationRequired implements KycBusinessReverificationRequirer {

    private static final String THE_BUSINESS = "";

    private final ProviderAccountRepository providers;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordBusinessReverificationRequired(ProviderAccountRepository providers, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordBusinessReverificationRequiredRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());

        provider.requireReverification(request.reason(), request.deadline());

        providers.save(provider);
        events.publish(new ReverificationRequiredIE(
                provider.id(), THE_BUSINESS, request.reason().strip(), provider.verifiedUntil().orElseThrow(), clock.instant()));
    }
}
