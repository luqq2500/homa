package account.usecase;

import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.usecase.dto.RegisterProviderRequest;
import account.usecase.dto.RegisterProviderResponse;
import account.usecase.exception.PhoneAlreadyRegisteredException;
import account.usecase.port.in.ProviderAccountRegistrator;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.ProviderAccountRepository;

/**
 * Registers a business with a confirmed phone number. It then verifies through KYB. The verification is
 * used up, so it registers no other account. No other context needs to know until KYB passes.
 */
public final class ProviderRegisterAccount implements ProviderAccountRegistrator {

    private final PhoneVerificationRepository verifications;
    private final ProviderAccountRepository providers;

    public ProviderRegisterAccount(PhoneVerificationRepository verifications, ProviderAccountRepository providers) {
        this.verifications = verifications;
        this.providers = providers;
    }

    @Override
    public RegisterProviderResponse register(RegisterProviderRequest request) {
        PhoneVerification verified = Accounts.verification(verifications, request.verificationId());
        if (providers.existsWithPhone(verified.phone().value())) {
            throw new PhoneAlreadyRegisteredException("provider");
        }

        ProviderAccount provider = ProviderAccount.register(verified);

        providers.save(provider);
        verifications.remove(verified.id());
        return new RegisterProviderResponse(provider.id(), provider.phone().value());
    }
}
