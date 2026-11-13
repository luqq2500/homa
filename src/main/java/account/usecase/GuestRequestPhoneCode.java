package account.usecase;

import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.usecase.dto.RequestPhoneCodeRequest;
import account.usecase.dto.RequestPhoneCodeResponse;
import account.usecase.port.in.GuestPhoneCodeRequester;
import account.usecase.port.out.CodeGenerator;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.SmsSender;
import java.time.Clock;

public final class GuestRequestPhoneCode implements GuestPhoneCodeRequester {

    private final PhoneVerificationRepository verifications;
    private final CodeGenerator codes;
    private final SmsSender sms;
    private final Clock clock;

    public GuestRequestPhoneCode(PhoneVerificationRepository verifications, CodeGenerator codes, SmsSender sms,
                                 Clock clock) {
        this.verifications = verifications;
        this.codes = codes;
        this.sms = sms;
        this.clock = clock;
    }

    @Override
    public RequestPhoneCodeResponse request(RequestPhoneCodeRequest request) {
        PhoneNumber phone = new PhoneNumber(request.phone());
        PhoneVerification verification = PhoneVerification.send(phone, codes.sixDigits(), clock.instant());
        verifications.save(verification);
        sms.sendCode(phone, verification.code());
        return new RequestPhoneCodeResponse(verification.id(), verification.expiresAt());
    }
}
