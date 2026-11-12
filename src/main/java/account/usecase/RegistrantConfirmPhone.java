package account.usecase;

import account.domain.PhoneVerification;
import account.usecase.dto.ConfirmPhoneRequest;
import account.usecase.dto.ConfirmPhoneResponse;
import account.usecase.port.in.RegistrantPhoneConfirmer;
import account.usecase.port.out.PhoneVerificationRepository;
import java.time.Clock;

/** Checks the code the person entered. A wrong code counts as an attempt, so it is saved too. */
public final class RegistrantConfirmPhone implements RegistrantPhoneConfirmer {

    private final PhoneVerificationRepository verifications;
    private final Clock clock;

    public RegistrantConfirmPhone(PhoneVerificationRepository verifications, Clock clock) {
        this.verifications = verifications;
        this.clock = clock;
    }

    @Override
    public ConfirmPhoneResponse confirm(ConfirmPhoneRequest request) {
        PhoneVerification verification = Accounts.verification(verifications, request.verificationId());

        boolean confirmed = verification.confirm(request.code(), clock.instant());

        verifications.save(verification);
        int attemptsLeft = PhoneVerification.MAX_WRONG_ATTEMPTS - verification.wrongAttempts();
        return new ConfirmPhoneResponse(verification.id(), confirmed, attemptsLeft);
    }
}
