package account.domain;

import account.domain.exception.InvalidVerificationCodeException;
import account.domain.exception.PhoneAlreadyConfirmedException;
import account.domain.exception.PhoneNotVerifiedException;
import account.domain.exception.TooManyVerificationAttemptsException;
import account.domain.exception.VerificationCodeExpiredException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Confirms that someone holds a phone number, with a 6-digit code sent by SMS. The code is valid for
 * 5 minutes; after 5 wrong codes a new one is needed.
 */
public final class PhoneVerification {

    public static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
    public static final int MAX_WRONG_ATTEMPTS = 5;

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    private final String id;
    private final PhoneNumber phone;
    private final String code;
    private final Instant expiresAt;
    private int wrongAttempts;
    private boolean confirmed;

    private PhoneVerification(String id, PhoneNumber phone, String code, Instant expiresAt) {
        this.id = id;
        this.phone = phone;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    /** Starts a verification with the code sent to {@code phone} at {@code now}. */
    public static PhoneVerification start(PhoneNumber phone, String code, Instant now) {
        if (code == null || !SIX_DIGITS.matcher(code).matches()) {
            throw new InvalidVerificationCodeException();
        }
        return new PhoneVerification(
                new PhoneVerificationId().generate(), Objects.requireNonNull(phone), code, now.plus(CODE_VALIDITY));
    }

    /**
     * Checks the code the person entered. A right code confirms the phone; a wrong one counts as an attempt.
     *
     * @return whether the phone is now confirmed
     */
    public boolean confirm(String entered, Instant now) {
        if (confirmed) {
            throw new PhoneAlreadyConfirmedException(phone.value());
        }
        if (wrongAttempts >= MAX_WRONG_ATTEMPTS) {
            throw new TooManyVerificationAttemptsException(wrongAttempts);
        }
        if (!now.isBefore(expiresAt)) {
            throw new VerificationCodeExpiredException(expiresAt);
        }
        confirmed = code.equals(entered);
        wrongAttempts += confirmed ? 0 : 1;
        return confirmed;
    }

    /** Accounts are registered only with a confirmed phone number. */
    void requireConfirmed() {
        if (!confirmed) {
            throw new PhoneNotVerifiedException(phone.value());
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int wrongAttempts() {
        return wrongAttempts;
    }

    public String id() {
        return id;
    }
}
