package com.homa.provider.domain;

import com.homa.provider.domain.exception.MemberNotVerifiedException;
import com.homa.provider.domain.exception.MemberUnderageException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A helper or supervisor of one provider: one aggregate per person. A member exists only once they have passed
 * verification (HLP-1 to HLP-4), so they start verified and active. Active means verified, not suspended by Homa
 * and not deactivated by the provider (§1). Deactivation and reactivation (HLP-5 to HLP-8) join in slice 4;
 * re-verification and suspension (MON-1 to MON-4) in slice 6.
 */
public final class TeamMember {

    private final TeamMemberId id;
    private final ProviderId providerId;
    private final TeamRole role;
    private final Eligibility eligibility;
    private final Instant verifiedAt;
    private final boolean deactivated;
    private final boolean suspended;

    private TeamMember(TeamMemberId id, ProviderId providerId, TeamRole role, Eligibility eligibility,
            Instant verifiedAt, boolean deactivated, boolean suspended) {
        this.id = Objects.requireNonNull(id, "id");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.role = Objects.requireNonNull(role, "role");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
        this.deactivated = deactivated;
        this.suspended = suspended;
    }

    /**
     * Adds a verified member to a provider's team (HLP-1 to HLP-4). Called through
     * {@link Provider#addTeamMember}, which first checks that the provider is a verified business (PRV-1).
     */
    static TeamMember add(TeamMemberId id, ProviderId providerId, TeamRole role, Eligibility eligibility,
            MemberVerification verification) {
        Objects.requireNonNull(verification, "verification");
        if (!verification.outcome().passed()) {
            throw new MemberNotVerifiedException(id.value());
        }
        if (!AgeRequirement.TEAM_MEMBER.isMetBy(verification.dateOfBirth(), verification.verifiedAt())) {
            throw new MemberUnderageException(id.value());
        }
        eligibility.requireAllowedToWorkAt(verification.verifiedAt(), id);
        return new TeamMember(id, providerId, role, eligibility, verification.verifiedAt(), false, false);
    }

    /** Rebuilds a member from what was stored. Applies no rule: the member was valid when it was saved. */
    public static TeamMember restore(TeamMemberId id, ProviderId providerId, TeamRole role, Eligibility eligibility,
            Instant verifiedAt, boolean deactivated, boolean suspended) {
        return new TeamMember(id, providerId, role, eligibility, verifiedAt, deactivated, suspended);
    }

    /** Verified, not suspended by Homa and not deactivated by the provider (§1). */
    public boolean isActive() {
        return !deactivated && !suspended;
    }

    /** An active member in the helper role: the ones that let a provider go live and be booked (PRV-2, PRV-6). */
    public boolean isActiveHelper() {
        return role == TeamRole.HELPER && isActive();
    }

    public TeamMemberId id() {
        return id;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public TeamRole role() {
        return role;
    }

    public Eligibility eligibility() {
        return eligibility;
    }

    /** The last Kuala Lumpur date a job this member is rostered on may end (ROS-4); empty for a citizen. */
    public Optional<LocalDate> rosterableUntil() {
        return eligibility.rosterableUntil();
    }

    /** When the member last passed verification. */
    public Instant verifiedAt() {
        return verifiedAt;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public boolean isSuspended() {
        return suspended;
    }
}
