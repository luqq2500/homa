package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.RecordBusinessLapseRequest;
import account.usecase.port.in.KycBusinessLapseRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.ProviderVerificationLapsedIE;
import java.time.Clock;

/**
 * The KYC provider reports that a business missed its re-verification: it lapses and takes no new
 * requests until it passes.
 */
public final class KycRecordBusinessLapse implements KycBusinessLapseRecorder {

    private final ProviderAccountRepository providers;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordBusinessLapse(ProviderAccountRepository providers, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordBusinessLapseRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());

        provider.lapse();

        providers.save(provider);
        events.publish(new ProviderVerificationLapsedIE(provider.id(), clock.instant()));
    }
}
