package com.homa.provider.domain;

import com.homa.provider.domain.exception.MemberUnderageException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A helper or supervisor of one provider: one aggregate per person, recognised by the personId Verification gave
 * them. A member is added only from what Verification decided about them (HLP-1 to HLP-3) and with the provider's
 * confirmation for a foreign member (HLP-4), so they start active. Active means verified, not suspended and not
 * deactivated by the provider (§1).
 *
 * <p>A member also carries whether their face photo is verified (IDN-4), from {@code ProfilePhotoChangedIE}. Only a
 * member with a verified photo can be rostered, and the customer sees it on the roster (ROS-6); Reservation applies
 * that from {@code photoVerified} on the team-member events.
 *
 * <p>Left for slice 4: deactivation and reactivation (HLP-5 to HLP-7), and removal (HLP-8, HLP-9), which refuses
 * inside a rostered window from the {@code RosteredWindows} replica, so a removed member returns only as a new
 * member. One person belongs to only one provider (HLP-10), checked through an {@code Affiliation} looked up by
 * personId, as Customer's {@code SignInStatus} is. Left for slice 6: suspension (PRV-12, MON-3, MON-4, PRV-9) and
 * permit renewal.
 */
public final class TeamMember {

    private final TeamMemberId id;
    private final ProviderId providerId;
    private final PersonId personId;
    private final TeamRole role;
    private final Eligibility eligibility;
    private final Instant addedAt;
    private boolean photoVerified;
    private final boolean deactivated;
    private final boolean suspended;

    private TeamMember(TeamMemberId id, ProviderId providerId, PersonId personId, TeamRole role,
            Eligibility eligibility, Instant addedAt, boolean photoVerified, boolean deactivated, boolean suspended) {
        this.id = Objects.requireNonNull(id, "id");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.personId = Objects.requireNonNull(personId, "personId");
        this.role = Objects.requireNonNull(role, "role");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt");
        this.photoVerified = photoVerified;
        this.deactivated = deactivated;
        this.suspended = suspended;
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
        return new TeamMember(id, providerId, person.personId(), role, eligibility, addedAt, false, false, false);
    }

    /** Rebuilds a member from what was stored. Applies no rule: the member was valid when it was saved. */
    public static TeamMember restore(TeamMemberId id, ProviderId providerId, PersonId personId, TeamRole role,
            Eligibility eligibility, Instant addedAt, boolean photoVerified, boolean deactivated, boolean suspended) {
        return new TeamMember(id, providerId, personId, role, eligibility, addedAt, photoVerified, deactivated,
                suspended);
    }

    /**
     * Takes Verification's latest word on the member's face photo (IDN-4): verified, or with no verified photo. A new
     * photo replaces the current one only once it is verified, so Verification decides when this changes.
     */
    public void recordPhotoVerification(boolean verified) {
        this.photoVerified = verified;
    }

    /** Verified, not suspended and not deactivated by the provider (§1). */
    public boolean isActive() {
        return !deactivated && !suspended;
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
}
