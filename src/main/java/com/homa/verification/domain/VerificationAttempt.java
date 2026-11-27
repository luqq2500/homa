package com.homa.verification.domain;

import com.homa.verification.domain.exception.IdentityNotVerifiedException;
import com.homa.verification.domain.exception.VerificationAccountMismatchException;
import com.homa.verification.domain.exception.VerificationAlreadyDecidedException;
import com.homa.verification.domain.exception.VerificationRuleViolationException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One verification a person started, for one role: the check the verification service runs and, once it answers,
 * Homa's decision (§11, EXT-1). The service answers later, so the attempt waits as {@code PENDING} and is decided
 * once: {@code VERIFIED}, or {@code REFUSED} under the rule that refused it (IDN-2 keeps the result as recorded).
 *
 * <p>A customer verifies for the customer account that asked, at its first reservation (CUS-3), so a customer's
 * attempt names that account; an owner's or a member's names none, since Provider recognises them by personId.
 *
 * <p>It holds the service's reference and no identity document, image or number (IDN-2, EXT-4); a refusal keeps
 * only the rule and its message, which never holds a number (IDN-3).
 *
 * <p>Hooks, not built yet: an attempt submitted during a verification-service outage stays pending and is
 * resubmitted when the service is back (EXT-5, slice 10); re-verification starts attempts of its own (MON-1, slice 6).
 */
public final class VerificationAttempt {

    private final VerificationAttemptId id;
    private final PersonId personId;
    private final PersonRole role;
    private final String accountId;
    private final Instant startedAt;
    private String serviceReference;
    private VerificationAttemptState state = VerificationAttemptState.PENDING;
    private String refusedUnder;
    private String refusalReason;
    private Instant decidedAt;

    private VerificationAttempt(VerificationAttemptId id, PersonId personId, PersonRole role, String accountId,
            Instant startedAt) {
        this.id = id;
        this.personId = personId;
        this.role = role;
        this.accountId = accountId;
        this.startedAt = startedAt;
    }

    /**
     * Starts a verification of {@code personId} for {@code role}. It is then submitted to the verification service
     * ({@link #submittedAs}), so nothing reaches the service for a verification Homa wouldn't start.
     *
     * @param accountId the customer account the verification is for when {@code role} is CUSTOMER; null otherwise
     * @throws VerificationAccountMismatchException when a customer's attempt names no account, or another role's
     *         names one (CUS-3)
     */
    public static VerificationAttempt start(VerificationAttemptId id, PersonId personId, PersonRole role,
            String accountId, Instant startedAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(personId, "personId");
        Objects.requireNonNull(startedAt, "startedAt");
        return new VerificationAttempt(id, personId, role, accountFor(Objects.requireNonNull(role, "role"), accountId),
                startedAt);
    }

    /** CUS-3: a customer's verification names their account; an owner's or member's names none. */
    private static String accountFor(PersonRole role, String accountId) {
        String account = namedAccount(accountId);
        if ((account != null) != (role == PersonRole.CUSTOMER)) {
            throw mismatchFor(role);
        }
        return account;
    }

    /** A blank account names none. */
    private static String namedAccount(String accountId) {
        return accountId == null || accountId.isBlank() ? null : accountId;
    }

    private static VerificationAccountMismatchException mismatchFor(PersonRole role) {
        return new VerificationAccountMismatchException(role == PersonRole.CUSTOMER
                ? "a customer verifies for their customer account"
                : role.description() + " verifies without a customer account");
    }

    /**
     * The verification service took the check and knows it by {@code reference}. Only a pending verification is
     * submitted; a resubmission after an outage (EXT-5, slice 10) replaces the reference.
     */
    public void submittedAs(String reference) {
        Objects.requireNonNull(reference, "reference");
        requirePending();
        serviceReference = reference;
    }

    /** The service passed the check and the person is verified for the role (IDN-1). */
    public void recordPass(Instant at) {
        decide(VerificationAttemptState.VERIFIED, at);
    }

    /** Homa refused the verification under {@code refusal}'s rule, for example IDN-3 or CUS-10. */
    public void recordRefusal(VerificationRuleViolationException refusal, Instant at) {
        Objects.requireNonNull(refusal, "refusal");
        decide(VerificationAttemptState.REFUSED, at);
        refusedUnder = refusal.ruleId();
        refusalReason = refusal.getMessage();
    }

    /** IDN-1: the verification service didn't pass the person's identity document, so they can't be verified. */
    public void recordUnpassedCheck(Instant at) {
        recordRefusal(new IdentityNotVerifiedException(), at);
    }

    private void decide(VerificationAttemptState decision, Instant at) {
        Objects.requireNonNull(at, "at");
        requirePending();
        state = decision;
        decidedAt = at;
    }

    private void requirePending() {
        if (state != VerificationAttemptState.PENDING) {
            throw new VerificationAlreadyDecidedException(state.name().toLowerCase());
        }
    }

    /** True while the verification service hasn't answered. */
    public boolean isPending() {
        return state == VerificationAttemptState.PENDING;
    }

    public VerificationAttemptId id() {
        return id;
    }

    public PersonId personId() {
        return personId;
    }

    public PersonRole role() {
        return role;
    }

    /** The customer account a customer's verification is for (CUS-3). Empty for an owner or a member. */
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    /** How the verification service knows this check. Empty until it is submitted. */
    public Optional<String> serviceReference() {
        return Optional.ofNullable(serviceReference);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public VerificationAttemptState state() {
        return state;
    }

    /** The rule a refusal was made under, such as "IDN-3". Empty unless refused. */
    public Optional<String> refusedUnder() {
        return Optional.ofNullable(refusedUnder);
    }

    /** Why it was refused, in the rule's words; never an identity number. Empty unless refused. */
    public Optional<String> refusalReason() {
        return Optional.ofNullable(refusalReason);
    }

    /** When it was verified or refused. Empty while pending. */
    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }
}
