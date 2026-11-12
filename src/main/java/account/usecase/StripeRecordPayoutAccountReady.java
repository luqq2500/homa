package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.RecordPayoutAccountReadyRequest;
import account.usecase.port.in.StripePayoutAccountReadyRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.ProviderAccountRepository;
import ie.events.PayoutAccountReadyIE;
import java.time.Clock;

/**
 * Records Stripe's report that a business's payout account is ready, and tells the other contexts. A
 * repeated report changes nothing, so nothing is saved or told.
 */
public final class StripeRecordPayoutAccountReady implements StripePayoutAccountReadyRecorder {

    private final ProviderAccountRepository providers;
    private final AccountIEPublisher events;
    private final Clock clock;

    public StripeRecordPayoutAccountReady(ProviderAccountRepository providers, AccountIEPublisher events, Clock clock) {
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordPayoutAccountReadyRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());

        if (provider.recordPayoutAccountReady()) {
            providers.save(provider);
            events.publish(new PayoutAccountReadyIE(provider.id(), clock.instant()));
        }
    }
}
