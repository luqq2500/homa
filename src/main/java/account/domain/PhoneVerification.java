package account.domain;

import account.domain.exception.CodeExpiredException;
import account.domain.exception.InvalidCodeException;
import account.domain.exception.PhoneNotVerifiedException;
import account.domain.exception.TooManyWrongCodesException;
import account.domain.exception.WrongCodeException;
import java.time.Duration;
import java.time.Instant;

/**
 * One SMS code sent to a phone number. The code is 6 digits and valid for 5 minutes; after 5 wrong
 * codes a new one is needed. A confirmed verification registers one account.
 */
public final class PhoneVerification {

    public static final Duration VALIDITY = Duration.ofMinutes(5);
    public static final int WRONG_CODES_ALLOWED = 5;

    private final String id;
    private final PhoneNumber phone;
    private final String code;
    private final Instant sentAt;
    private int wrongCodes;
    private boolean confirmed;
    private boolean used;

    public PhoneVerification(String id, PhoneNumber phone, String code, Instant sentAt, int wrongCodes,
                             boolean confirmed, boolean used) {
        if (code == null || !code.matches("\\d{6}")) {
            throw new InvalidCodeException();
        }
        this.id = id;
        this.phone = phone;
        this.code = code;
        this.sentAt = sentAt;
        this.wrongCodes = wrongCodes;
        this.confirmed = confirmed;
        this.used = used;
    }

    public static PhoneVerification send(PhoneNumber phone, String code, Instant now) {
        return new PhoneVerification(new PhoneVerificationId().generate(), phone, code, now, 0, false, false);
    }

    /** Checks the code the person typed. A wrong code is counted before it is refused. */
    public void confirm(String typedCode, Instant now) {
        if (wrongCodes >= WRONG_CODES_ALLOWED) {
            throw new TooManyWrongCodesException();
        }
        if (now.isAfter(expiresAt())) {
            throw new CodeExpiredException();
        }
        if (!code.equals(typedCode)) {
            wrongCodes++;
            throw new WrongCodeException();
        }
        confirmed = true;
    }

    /** Hands over the verified phone number to register an account with; a verification registers one account. */
    public PhoneNumber useToRegister() {
        if (!confirmed || used) {
            throw new PhoneNotVerifiedException();
        }
        used = true;
        return phone;
    }

    /** The last moment the code is still valid. */
    public Instant expiresAt() {
        return sentAt.plus(VALIDITY);
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String code() {
        return code;
    }

    public int wrongCodes() {
        return wrongCodes;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isUsed() {
        return used;
    }
}
