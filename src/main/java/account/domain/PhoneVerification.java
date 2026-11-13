package account.domain;

import account.domain.exception.PhoneCodeNotAcceptedException;
import account.domain.exception.PhoneNotVerifiedException;

/**
 * The verification of one phone number by SMS code. The SMS provider sends and checks the code by
 * its own rules; Homa records that the number was confirmed. A confirmed verification registers one account.
 */
public final class PhoneVerification {

    private final String id;
    private final PhoneNumber phone;
    private boolean confirmed;
    private boolean used;

    public PhoneVerification(String id, PhoneNumber phone, boolean confirmed, boolean used) {
        this.id = id;
        this.phone = phone;
        this.confirmed = confirmed;
        this.used = used;
    }

    public static PhoneVerification start(PhoneNumber phone) {
        return new PhoneVerification(new PhoneVerificationId().generate(), phone, false, false);
    }

    /** Only a code the SMS provider passed confirms the phone number. */
    public void confirm(CodeCheck check) {
        if (check != CodeCheck.PASSED) {
            throw new PhoneCodeNotAcceptedException(check.name());
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

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isUsed() {
        return used;
    }
}
