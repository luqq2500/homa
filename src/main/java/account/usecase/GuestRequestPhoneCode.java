package account.usecase;

import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.dto.RequestPhoneCodeResponse;
import account.usecase.port.in.GuestPhoneCodeRequester;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.PhoneVerifier;

public final class GuestRequestPhoneCode implements GuestPhoneCodeRequester {

    private final PhoneVerificationRepository verifications;
    private final PhoneVerifier verifier;

    public GuestRequestPhoneCode(PhoneVerificationRepository verifications, PhoneVerifier verifier) {
        this.verifications = verifications;
        this.verifier = verifier;
    }

    @Override
    public RequestPhoneCodeResponse request(RequestPhoneCodeRequest request) {
        PhoneNumber phone = new PhoneNumber(request.phone());
        PhoneVerification verification = PhoneVerification.start(phone);
        verifications.save(verification);
        verifier.sendCode(phone);
        return new RequestPhoneCodeResponse(verification.id());
    }
}
