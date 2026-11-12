package account.usecase;

import account.domain.BusinessRegistration;
import account.domain.ProviderAccount;
import account.usecase.dto.ProviderViewAccountRequest;
import account.usecase.dto.ProviderViewAccountResponse;
import account.usecase.port.in.ProviderAccountViewer;
import account.usecase.port.out.ProviderAccountRepository;
import java.time.Instant;

/** Shows a business its own account: its verification and whether its own part of going live is done. */
public final class ProviderViewAccount implements ProviderAccountViewer {

    private static final String NOT_YET = "";

    private final ProviderAccountRepository providers;

    public ProviderViewAccount(ProviderAccountRepository providers) {
        this.providers = providers;
    }

    @Override
    public ProviderViewAccountResponse view(ProviderViewAccountRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());
        return new ProviderViewAccountResponse(
                provider.id(),
                provider.phone().value(),
                provider.verificationStatus().name(),
                provider.businessRegistration().map(BusinessRegistration::ssmNumber).orElse(NOT_YET),
                provider.businessRegistration().map(BusinessRegistration::registeredName).orElse(NOT_YET),
                provider.verifiedUntil().map(Instant::toString).orElse(NOT_YET),
                provider.isPayoutAccountReady(),
                provider.isReadyForGoLive());
    }
}
