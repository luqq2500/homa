package com.homa.verification.domain;

import com.homa.verification.domain.exception.IdentityNotVerifiedException;
import com.homa.verification.domain.exception.PersonUnderageException;
import com.homa.verification.domain.exception.StatusNotAllowedForRoleException;
import com.homa.verification.domain.exception.WorkPermitExpiryMissingException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * One real person whose identity Homa has verified, whatever their roles (customer, owner, member), keyed by
 * {@link PersonId}. It exists only through {@link #verify}, so a verified person always passed with the identity
 * document the country policy sets for their status (IDN-1) and is eligible for the role they verified for.
 *
 * <p>It holds only what IDN-2 lists: name, local or foreign status, the identity document (type and number), the
 * verification result with its age check and, for a foreign person, the work permit expiry. No date of birth, no
 * document image and no biometric data; there is no field that could hold them (IDN-2, EXT-4). The identity number
 * is used only to recognise the person (IDN-3): it stays inside, is read only through {@link #identityNumber()},
 * and never appears in {@link #toString()}.
 *
 * <p>Hooks, not built yet: the verified profile photo of an owner or member, replaced only once a new one passes
 * (IDN-4, slice 9); re-verification results and lapsing (MON-1 to MON-3, slice 6); monitoring flags, which only
 * Support clears (MON-4, slice 6).
 */
public final class VerifiedPerson {

    private final PersonId id;
    private final PersonName name;
    private final PersonStatus status;
    private final IdentityDocument identityDocument;
    private final VerificationResult result;
    private final LocalDate permitExpiry;

    private VerifiedPerson(PersonId id, PersonName name, PersonStatus status, IdentityDocument identityDocument,
            VerificationResult result, LocalDate permitExpiry) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.identityDocument = identityDocument;
        this.result = result;
        this.permitExpiry = permitExpiry;
    }

    /**
     * Verifies person {@code id} for {@code role} from the verification service's {@code check}, under the country
     * {@code policy}. {@code numberStatus} is what Homa already knows about the check's identity number.
     *
     * @throws IdentityNotVerifiedException when the check didn't pass (IDN-1)
     * @throws com.homa.verification.domain.exception.DocumentNotSetForStatusException when the document isn't the
     *         one the policy sets for the person's status (IDN-1)
     * @throws com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException when a different person
     *         already holds the number (IDN-3, CUS-10, HLP-10)
     * @throws WorkPermitExpiryMissingException when a foreign person's document gives no permit expiry (HLP-3)
     * @throws PersonUnderageException when the person is under 18 (CUS-10, PRV-1, HLP-1)
     * @throws StatusNotAllowedForRoleException when the policy doesn't allow the status for the role (CUS-10, PRV-1)
     */
    public static VerifiedPerson verify(PersonId id, PersonRole role, IdentityCheck check, CountryPolicy policy,
            IdentityNumberStatus numberStatus) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        if (check.outcome() != VerificationOutcome.PASSED) {
            throw new IdentityNotVerifiedException();
        }
        AcceptedDocument document = policy.requireDocumentFor(check.status(), check.document().type());
        numberStatus.admit(id);
        boolean ofAge = AgeRequirement.ADULT.isMetBy(check.dateOfBirth(), check.checkedAt());
        VerifiedPerson person = new VerifiedPerson(id, check.name(), check.status(), check.document(),
                new VerificationResult(check.outcome(), check.checkedAt(), ofAge),
                permitExpiryFrom(check, document));
        person.requireEligibleAs(role, policy);
        return person;
    }

    /** §14, HLP-3: a foreign person's permit expiry is their document's date, where the policy says it dates it. */
    private static LocalDate permitExpiryFrom(IdentityCheck check, AcceptedDocument document) {
        if (check.status() == PersonStatus.LOCAL) {
            return null;
        }
        if (!document.datesWorkPermit() || check.documentExpiry() == null) {
            throw new WorkPermitExpiryMissingException(document.type().name());
        }
        return check.documentExpiry();
    }

    /**
     * Refuses {@code role} for this person unless they are aged 18 or above and the country policy allows their
     * status for it (CUS-10, PRV-1, HLP-1 to HLP-3). Used at verification, and when an already verified person takes
     * another role, such as a member who becomes a customer.
     */
    public void requireEligibleAs(PersonRole role, CountryPolicy policy) {
        if (!result.ageCheckPassed()) {
            throw new PersonUnderageException(role.ruleId(), role.description(), AgeRequirement.ADULT.minimumAge());
        }
        if (!policy.allows(role, status)) {
            throw new StatusNotAllowedForRoleException(role.ruleId(), role.description(),
                    status.name().toLowerCase(), policy.country());
        }
    }

    /**
     * When this person next re-verifies in {@code role}: a cycle after their last pass for an owner or a member
     * (MON-1); empty for a customer, who doesn't re-verify. Published as {@code nextDueAt}.
     */
    public Optional<Instant> nextDueAt(PersonRole role) {
        if (!role.reverifies()) {
            return Optional.empty();
        }
        return Optional.of(VerificationCycle.EVERY_SIX_MONTHS.nextDueAt(result.checkedAt()));
    }

    public PersonId id() {
        return id;
    }

    public PersonName name() {
        return name;
    }

    public PersonStatus status() {
        return status;
    }

    /** The kind of identity document the person verified with. */
    public IdentityDocumentType identityDocumentType() {
        return identityDocument.type();
    }

    /**
     * IDN-3: the identity document number, only for storing it, for Support's recorded view (DAT-14) and for the
     * payout-account holder check. Never put it in an event, a response or a log.
     */
    public IdentityNumber identityNumber() {
        return identityDocument.number();
    }

    public VerificationResult result() {
        return result;
    }

    /** A foreign person's work permit expiry, from their i-Kad in Malaysia (§14, HLP-3). Empty for a local person. */
    public Optional<LocalDate> permitExpiry() {
        return Optional.ofNullable(permitExpiry);
    }

    /** Names the person by id and status only; never the identity number (IDN-3), nor the name. */
    @Override
    public String toString() {
        return "VerifiedPerson[id=" + id.value() + ", status=" + status + ", document=" + identityDocument.type().name()
                + ", result=" + result + "]";
    }
}
