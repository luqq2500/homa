package account.usecase.port.out;

/** Stripe's onboarding for a business's payout account. Stripe reports back when the account is ready. */
public interface PayoutAccountService {

    /** Returns the address of Stripe's onboarding page for the business. */
    String startOnboarding(String providerId);
}
