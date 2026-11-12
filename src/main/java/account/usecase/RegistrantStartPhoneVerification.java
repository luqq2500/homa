package account.usecase;

import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.usecase.dto.StartPhoneVerificationRequest;
import account.usecase.dto.StartPhoneVerificationResponse;
import account.usecase.port.in.RegistrantPhoneVerificationStarter;
import account.usecase.port.out.PhoneVerificationRepository;
import account.usecase.port.out.SmsSender;
import account.usecase.port.out.VerificationCodes;
import java.time.Clock;

/**
 * Starts a phone verification and sends its 6-digit code by SMS. The code is sent only after the
 * verification is saved, so a code that arrives can always be confirmed.
 */
public final class RegistrantStartPhoneVerification implements RegistrantPhoneVerificationStarter {

    private final PhoneVerificationRepository verifications;
    private final VerificationCodes codes;
    private final SmsSender sms;
    private final Clock clock;

    public RegistrantStartPhoneVerification(
            PhoneVerificationRepository verifications, VerificationCodes codes, SmsSender sms, Clock clock) {
        this.verifications = verifications;
        this.codes = codes;
        this.sms = sms;
        this.clock = clock;
    }

    @Override
    public StartPhoneVerificationResponse start(StartPhoneVerificationRequest request) {
        PhoneNumber phone = PhoneNumber.of(request.phone());
        String code = codes.newCode();

        PhoneVerification verification = PhoneVerification.start(phone, code, clock.instant());

        verifications.save(verification);
        sms.sendVerificationCode(phone.value(), code);
        return new StartPhoneVerificationResponse(verification.id(), verification.expiresAt());
    }
}
