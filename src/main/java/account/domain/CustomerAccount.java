package account.domain;

import account.domain.exception.AccessNotesTooLongException;
import account.domain.exception.AccountClosedException;
import account.domain.exception.NameMissingException;
import java.time.Instant;
import java.util.Optional;

/** A customer's account: who they are, where their home is, and the access notes for the team. */
public final class CustomerAccount {

    public static final int ACCESS_NOTES_LIMIT = 500;

    private final String id;
    private final PhoneNumber phone;
    private String name;
    private Address address;
    private String accessNotes;
    private Language language;
    private Instant closedAt;

    public CustomerAccount(String id, PhoneNumber phone, String name, Address address, String accessNotes,
                           Language language, Instant closedAt) {
        this.id = id;
        this.phone = phone;
        this.name = required(name);
        this.address = address;
        this.accessNotes = withinLimit(accessNotes);
        this.language = language;
        this.closedAt = closedAt;
    }

    /** Registers a customer with the phone number they just verified. */
    public static CustomerAccount register(PhoneVerification verification, String name, Address address,
                                           Language language) {
        String checkedName = required(name);
        return new CustomerAccount(new CustomerAccountId().generate(), verification.useToRegister(), checkedName,
                address, "", language, null);
    }

    public void changeName(String name) {
        requireOpen();
        this.name = required(name);
    }

    /** The new address is used for new searches and requests; those already made keep theirs. */
    public void changeAddress(Address address) {
        requireOpen();
        this.address = address;
    }

    public void changeAccessNotes(String accessNotes) {
        requireOpen();
        this.accessNotes = withinLimit(accessNotes);
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

    private static String required(String name) {
        if (name == null || name.isBlank()) {
            throw new NameMissingException();
        }
        return name.strip();
    }

    private static String withinLimit(String accessNotes) {
        String notes = accessNotes == null ? "" : accessNotes;
        if (notes.length() > ACCESS_NOTES_LIMIT) {
            throw new AccessNotesTooLongException(ACCESS_NOTES_LIMIT);
        }
        return notes;
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String name() {
        return name;
    }

    public Address address() {
        return address;
    }

    public String accessNotes() {
        return accessNotes;
    }

    public Language language() {
        return language;
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
