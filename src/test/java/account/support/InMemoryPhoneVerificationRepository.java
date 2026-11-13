package account.support;

import account.domain.PhoneVerification;
import account.usecase.port.out.PhoneVerificationRepository;
import support.InMemoryStore;

public final class InMemoryPhoneVerificationRepository extends InMemoryStore<PhoneVerification> implements PhoneVerificationRepository {

    public InMemoryPhoneVerificationRepository() {
        super(PhoneVerification::id);
    }
}
