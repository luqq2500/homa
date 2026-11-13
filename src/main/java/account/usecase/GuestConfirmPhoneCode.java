package account.usecase;

import account.domain.BusinessAccount;
import account.domain.CustomerAccount;
import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.usecase.dto.ConfirmPhoneCodeRequest;
import account.usecase.dto.ConfirmPhoneCodeResponse;
import account.usecase.exception.PhoneVerificationNotFoundException;
import account.usecase.port.in.GuestPhoneCodeConfirmer;
import account.usecase.port.out.BusinessAccountRepository;
import account.usecase.port.out.CustomerAccountRepository;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.PhoneVerifier;

public final class GuestConfirmPhoneCode implements GuestPhoneCodeConfirmer {

    private final PhoneVerificationRepository verifications;
    private final CustomerAccountRepository customers;
    private final BusinessAccountRepository businesses;
    private final PhoneVerifier verifier;

    public GuestConfirmPhoneCode(PhoneVerificationRepository verifications, CustomerAccountRepository customers,
                                 BusinessAccountRepository businesses, PhoneVerifier verifier) {
        this.verifications = verifications;
        this.customers = customers;
        this.businesses = businesses;
        this.verifier = verifier;
    }

    /** The response's account ids are null where the phone number has no such account yet. */
    @Override
    public ConfirmPhoneCodeResponse confirm(ConfirmPhoneCodeRequest request) {
        PhoneVerification verification = verifications.find(request.verificationId())
                .orElseThrow(() -> new PhoneVerificationNotFoundException(request.verificationId()));
        PhoneNumber phone = verification.phone();
        verification.confirm(verifier.checkCode(phone, request.code()));
        verifications.save(verification);
        return new ConfirmPhoneCodeResponse(phone.value(),
                customers.findByPhone(phone).map(CustomerAccount::id).orElse(null),
                businesses.findByPhone(phone).map(BusinessAccount::id).orElse(null));
    }
}
