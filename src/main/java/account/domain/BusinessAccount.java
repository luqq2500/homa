package account.domain;

import account.domain.exception.AccountClosedException;
import java.time.Instant;
import java.util.Optional;

/** A business's account, registered with a phone number. Its id is the provider's id everywhere in Homa. */
public final class BusinessAccount {

    private final String id;
    private final PhoneNumber phone;
    private Language language;
    private Instant closedAt;

    public BusinessAccount(String id, PhoneNumber phone, Language language, Instant closedAt) {
        this.id = id;
        this.phone = phone;
        this.language = language;
        this.closedAt = closedAt;
    }

    /** Registers a business with the phone number just verified. */
    public static BusinessAccount register(PhoneVerification verification, Language language) {
        return new BusinessAccount(new BusinessAccountId().generate(), verification.useToRegister(), language, null);
    }

    public void chooseLanguage(Language language) {
        requireOpen();
        this.language = language;
    }

    public void close(Instant now) {
        requireOpen();
        this.closedAt = now;
    }

    private void requireOpen() {
        if (closedAt != null) {
            throw new AccountClosedException(id);
        }
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public Language language() {
        return language;
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
