package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.StartKybRequest;
import account.usecase.dto.StartKybResponse;
import account.usecase.port.in.ProviderKybStarter;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.ProviderAccountRepository;

/**
 * Opens the KYC provider's KYB pages for the business, with its owner's or director's KYC in the same
 * flow. The same pages re-verify a verified or lapsed business. Nothing changes in Homa until the KYC
 * provider reports the result.
 */
public final class ProviderStartKyb implements ProviderKybStarter {

    private final ProviderAccountRepository providers;
    private final IdentityVerificationService kyc;

    public ProviderStartKyb(ProviderAccountRepository providers, IdentityVerificationService kyc) {
        this.providers = providers;
        this.kyc = kyc;
    }

    @Override
    public StartKybResponse start(StartKybRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());
        return new StartKybResponse(kyc.startKyb(provider.id()));
    }
}
