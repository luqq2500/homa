package account.usecase.port.in;

import account.usecase.dto.AddPayoutAccountRequest;
import account.usecase.dto.AddPayoutAccountResponse;

/** A business opens Stripe's onboarding to add its payout account. */
public interface ProviderPayoutAccountAdder {

    AddPayoutAccountResponse add(AddPayoutAccountRequest request);
}
