package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.RecordBusinessFlagRequest;
import account.usecase.port.in.KycBusinessFlagRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.ProviderFlaggedIE;
import java.time.Clock;

/**
 * The KYC provider's monitoring reports a confirmed serious flag on a business, such as being struck off
 * or a sanctions match: it lapses at once. The other contexts act on the event: its accepted reservations
 * are disputed and its payouts held (R10).
 */
public final class KycRecordBusinessFlag implements KycBusinessFlagRecorder {

    private final ProviderAccountRepository providers;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordBusinessFlag(ProviderAccountRepository providers, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordBusinessFlagRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());

        provider.recordSeriousFlag(request.reason());

        providers.save(provider);
        events.publish(new ProviderFlaggedIE(provider.id(), provider.seriousFlag().orElseThrow(), clock.instant()));
    }
}
