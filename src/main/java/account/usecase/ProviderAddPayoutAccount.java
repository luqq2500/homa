package account.usecase;

import account.domain.ProviderAccount;
import account.usecase.dto.AddPayoutAccountRequest;
import account.usecase.dto.AddPayoutAccountResponse;
import account.usecase.port.in.ProviderPayoutAccountAdder;
import account.usecase.port.out.PayoutAccountService;
import account.usecase.port.out.ProviderAccountRepository;

/**
 * Opens Stripe's onboarding for the business's payout account, a business bank account. Homa never sees
 * the bank details; Stripe reports when the account is ready.
 */
public final class ProviderAddPayoutAccount implements ProviderPayoutAccountAdder {

    private final ProviderAccountRepository providers;
    private final PayoutAccountService stripe;

    public ProviderAddPayoutAccount(ProviderAccountRepository providers, PayoutAccountService stripe) {
        this.providers = providers;
        this.stripe = stripe;
    }

    @Override
    public AddPayoutAccountResponse add(AddPayoutAccountRequest request) {
        ProviderAccount provider = Accounts.provider(providers, request.providerId());
        return new AddPayoutAccountResponse(stripe.startOnboarding(provider.id()));
    }
}
