package account.usecase;

import account.domain.CustomerAccount;
import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.usecase.exception.CustomerNotFoundException;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.exception.ProviderNotFoundException;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.ProviderAccountRepository;

/** Loading what an account usecase works on, or saying it is not there. */
final class Accounts {

    private Accounts() {
    }

    static PhoneVerification verification(PhoneVerificationRepository verifications, String verificationId) {
        return verifications.find(verificationId).orElseThrow(() -> new PhoneVerificationNotFoundException(verificationId));
    }

    static CustomerAccount customer(CustomerAccountRepository customers, String customerId) {
        return customers.find(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    static ProviderAccount provider(ProviderAccountRepository providers, String providerId) {
        return providers.find(providerId).orElseThrow(() -> new ProviderNotFoundException(providerId));
    }
}
