package com.homa.provider.domain;

import com.homa.provider.domain.exception.MemberAlreadyDeactivatedException;
import com.homa.provider.domain.exception.MemberInRosteredWindowException;
import com.homa.provider.domain.exception.MemberNotDeactivatedException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.domain.exception.MemberUnderageException;
import com.homa.provider.domain.exception.VerificationLapsedException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A helper or supervisor of one provider: one aggregate per person on the team, recognised by the personId
 * Verification gave them. A member is added only from what Verification decided about them (HLP-1 to HLP-3) and with
 * the provider's confirmation for a foreign member (HLP-4), so they start active. Active means verified, not suspended
 * and not deactivated by the provider (§1).
 *
 * <p>A member also carries whether their face photo is verified (IDN-4), from {@code ProfilePhotoChangedIE}. Only a
 * member with a verified photo can be rostered, and the customer sees it on the roster (ROS-6); Reservation applies
 * that from {@code photoVerified} on the team-member events.
 *
 * <p>The provider can deactivate a member and reactivate them (HLP-5). A deactivated member isn't active, so can't be
 * rostered (HLP-6), and is reactivated only with a valid verification (HLP-7): the member keeps when their last pass
 * falls due for re-verification, and Verification's latest word replaces it. The provider can also remove a member
 * (HLP-8), but not inside the Check-in Period or job time of a reservation they're rostered on (HLP-9). Removal is
 * final: nothing more is done to a removed member, and the person returns only as a new member, verified again. A
 * removed member stays stored as history and belongs to no provider (HLP-10).
 *
 * <p>One person belongs to only one provider (HLP-10), checked through an {@link Affiliation} when they join, in
 * {@link Provider#addTeamMember}. Left for slice 6: suspension (PRV-12, MON-3, MON-4, PRV-9) and permit renewal.
 */
public final class TeamMember {

    private final TeamMemberId id;
    private final ProviderId providerId;
    private final PersonId personId;
    private final TeamRole role;
    private final Eligibility eligibility;
    private final Instant addedAt;
    private boolean photoVerified;
    private boolean deactivated;
    private final boolean suspended;
    private boolean removed;
    private Instant verifiedUntil;

    private TeamMember(TeamMemberId id, ProviderId providerId, PersonId personId, TeamRole role,
            Eligibility eligibility, Instant addedAt, boolean photoVerified, boolean deactivated, boolean suspended,
            boolean removed, Instant verifiedUntil) {
        this.id = Objects.requireNonNull(id, "id");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.personId = Objects.requireNonNull(personId, "personId");
        this.role = Objects.requireNonNull(role, "role");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt");
        this.photoVerified = photoVerified;
        this.deactivated = deactivated;
        this.suspended = suspended;
        this.removed = removed;
        this.verifiedUntil = Objects.requireNonNull(verifiedUntil, "verifiedUntil");
    }

    /**
     * Adds a verified person to a provider's team at {@code addedAt} (HLP-1 to HLP-4). Called through
     * {@link Provider#addTeamMember}. The member's photo counts as unverified until Verification says otherwise
     * ({@link #recordPhotoVerification}).
     *
     * @throws MemberUnderageException when the person did not pass the age check (HLP-1)
     */
    static TeamMember add(TeamMemberId id, ProviderId providerId, TeamRole role, VerifiedPersonFacts person,
            boolean employmentConfirmed, Instant addedAt) {
        if (!person.ageCheckPassed()) {
            throw new MemberUnderageException(id.value());
        }
        Eligibility eligibility = Eligibility.of(person, employmentConfirmed);
        eligibility.requireAllowedToWorkAt(addedAt, id);
        return new TeamMember(id, providerId, person.personId(), role, eligibility, addedAt, false, false, false,
                false, person.verifiedUntil());
    }

    /**
     * Rebuilds a member from what was stored. Applies no rule: the member was valid when it was saved.
     * {@code verifiedUntil} is when the member's last verification falls due for re-verification (HLP-7).
     */
    public static TeamMember restore(TeamMemberId id, ProviderId providerId, PersonId personId, TeamRole role,
            Eligibility eligibility, Instant addedAt, boolean photoVerified, boolean deactivated, boolean suspended,
            boolean removed, Instant verifiedUntil) {
        return new TeamMember(id, providerId, personId, role, eligibility, addedAt, photoVerified, deactivated,
                suspended, removed, verifiedUntil);
    }

    /**
     * Takes Verification's latest word on the member's face photo (IDN-4): verified, or with no verified photo. A new
     * photo replaces the current one only once it is verified, so Verification decides when this changes.
     */
    public void recordPhotoVerification(boolean verified) {
        this.photoVerified = verified;
    }

    /**
     * Takes Verification's latest word on when the member's verification falls due for re-verification (MON-1), from a
     * new pass ({@code PersonVerifiedIE.nextDueAt}). Reactivation needs it to lie ahead (HLP-7).
     */
    public void recordVerification(Instant verifiedUntil) {
        this.verifiedUntil = Objects.requireNonNull(verifiedUntil, "verifiedUntil");
    }

    /**
     * The provider deactivates the member, for example for two weeks' leave (HLP-5). They are no longer active, so
     * can't be rostered (HLP-6), and stay on the team: they still belong to the provider (HLP-10).
     *
     * @throws MemberRemovedException            when the member was removed (HLP-8)
     * @throws MemberAlreadyDeactivatedException when the member is already deactivated
     */
    public void deactivate() {
        requireNotRemoved();
        if (deactivated) {
            throw new MemberAlreadyDeactivatedException(id.value());
        }
        this.deactivated = true;
    }

    /**
     * The provider reactivates a deactivated member at {@code now}, only with a valid verification: one that hasn't
     * fallen due for re-verification yet (HLP-7). It falls due at {@code verifiedUntil}, so at that moment it has
     * lapsed. A member who is also suspended stays inactive (slice 6).
     *
     * @throws MemberRemovedException        when the member was removed (HLP-8)
     * @throws MemberNotDeactivatedException when the member isn't deactivated
     * @throws VerificationLapsedException   when the member's verification has fallen due (HLP-7)
     */
    public void reactivate(Instant now) {
        requireNotRemoved();
        if (!deactivated) {
            throw new MemberNotDeactivatedException(id.value());
        }
        if (!now.isBefore(verifiedUntil)) {
            throw new VerificationLapsedException(id.value(), verifiedUntil);
        }
        this.deactivated = false;
    }

    /**
     * The provider removes the member at {@code now}, as it must for one who no longer works for it (HLP-8). It is
     * refused while they are in the Check-in Period or job time of a reservation they're rostered on (HLP-9): the
     * windows start at the Check-in Period and end at the job end, which is itself outside. Removal is final: the
     * person can only come back as a new member, verified again.
     *
     * @throws MemberRemovedException          when the member was removed already (HLP-8)
     * @throws MemberInRosteredWindowException when {@code now} is inside one of the member's windows (HLP-9)
     */
    public void remove(RosteredWindows windows, Instant now) {
        requireNotRemoved();
        if (windows.isInsideOneAt(now)) {
            throw new MemberInRosteredWindowException(id.value());
        }
        this.removed = true;
    }

    private void requireNotRemoved() {
        if (removed) {
            throw new MemberRemovedException(id.value());
        }
    }

    /** Verified, not suspended and not deactivated by the provider, and still on the team (§1). */
    public boolean isActive() {
        return !removed && !deactivated && !suspended;
    }

    /** An active member in the helper role: the ones that let a provider go live and be booked (PRV-2). */
    public boolean isActiveHelper() {
        return role == TeamRole.HELPER && isActive();
    }

    public TeamMemberId id() {
        return id;
    }

    public ProviderId providerId() {
        return providerId;
    }

    /** The person, as Verification knows them. */
    public PersonId personId() {
        return personId;
    }

    public TeamRole role() {
        return role;
    }

    public Eligibility eligibility() {
        return eligibility;
    }

    /** The last Kuala Lumpur date a job this member is rostered on may end (ROS-4); empty for a local member. */
    public Optional<LocalDate> rosterableUntil() {
        return eligibility.rosterableUntil();
    }

    /** When the member was added to the team. */
    public Instant addedAt() {
        return addedAt;
    }

    /** True when the member's face photo is verified (IDN-4); only then can they be rostered (ROS-6). */
    public boolean hasVerifiedPhoto() {
        return photoVerified;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public boolean isSuspended() {
        return suspended;
    }

    /** True once the provider removed the member (HLP-8); a removed member belongs to no provider (HLP-10). */
    public boolean isRemoved() {
        return removed;
    }

    /** When the member's last verification falls due for re-verification (MON-1, HLP-7). */
    public Instant verifiedUntil() {
        return verifiedUntil;
    }
}
