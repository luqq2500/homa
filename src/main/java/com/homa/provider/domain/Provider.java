package com.homa.provider.domain;

import com.homa.provider.domain.exception.NoActiveHelperException;
import com.homa.provider.domain.exception.NoVerifiedPayoutAccountException;
import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import com.homa.provider.domain.exception.ProviderAlreadyLiveException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A homecare provider on Homa, run by one owner (PRV-1). It is opened by an identity-verified owner, builds its team
 * from verified people (HLP-1 to HLP-4), and goes live with a verified payout account in its name and at least one
 * active helper (PRV-2); supervisors are optional (PRV-3). Business verification is not Provider's: it is
 * Verification's {@code BusinessVerification} (PRV-7).
 *
 * <p>One provider per person (HLP-10): {@link #open} and {@link #addTeamMember} take an {@link Affiliation}, looked up by
 * personId the way Customer's {@code SignInStatus} is, and refuse a person who already owns or belongs to a provider.
 * The owner can also work in their own team (PRV-6), through {@link #addOwnerToTeam}. After a member is deactivated,
 * reactivated or removed (HLP-5 to HLP-9), {@link #bookabilityFor} is asked again (PRV-2).
 *
 * <p>Left for slice 6:
 * <ul>
 *   <li>Slice 6: Business status (PRV-7), derived from {@code BusinessVerificationChangedIE} (business and owner link
 *       verified) and a payout account held by {@link PayoutAccountHolder#BUSINESS}; foreign members only for a
 *       Business provider (PRV-8) and their suspension on its loss (PRV-9); suspension, including the owner's
 *       (PRV-12, SUS-5, MON-6), which adds SUSPENDED to {@link Bookability}; closing (PRV-5), which adds CLOSING;
 *       payout account changes (PRV-10); insurance (PRV-4); and {@code open} refusing an owner or business with debt
 *       (PAY-9, PAY-11).</li>
 * </ul>
 */
public final class Provider {

    private final ProviderId id;
    private final Owner owner;
    private ProviderStatus status;
    private PayoutAccountHolder verifiedPayoutAccount;

    private Provider(ProviderId id, Owner owner, ProviderStatus status, PayoutAccountHolder verifiedPayoutAccount) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.status = Objects.requireNonNull(status, "status");
        this.verifiedPayoutAccount = verifiedPayoutAccount;
    }

    /**
     * A verified person opens a provider account as its owner (PRV-1). The owner must be verified as local and aged
     * 18 or above; see {@link Owner#verifiedAs}. They must also belong to no provider yet, as its owner or a member
     * (HLP-10): {@code affiliation} is what the caller found when it looked the person up.
     *
     * @throws PersonAlreadyBelongsToProviderException when the person already owns a provider or is on a team
     */
    public static Provider open(ProviderId id, VerifiedPersonFacts owner, Affiliation affiliation) {
        Owner verified = Owner.verifiedAs(owner);
        affiliation.admitOpeningProvider(verified.personId());
        return new Provider(id, verified, ProviderStatus.OPENED, null);
    }

    /**
     * Rebuilds a provider from what was stored. {@code verifiedPayoutAccount} is null while it has no verified payout
     * account. Applies no rule: the provider was valid when it was saved.
     */
    public static Provider restore(ProviderId id, Owner owner, ProviderStatus status,
            PayoutAccountHolder verifiedPayoutAccount) {
        return new Provider(id, owner, status, verifiedPayoutAccount);
    }

    /**
     * Adds a verified person to the team at {@code addedAt}, before or after going live (PRV-3: a supervisor can join
     * later). The member must have passed the age check (HLP-1); a local member is verified as local (HLP-2); a
     * foreign member needs at least 90 days left on their permit (HLP-3) and the provider's confirmation that they
     * work lawfully for it (HLP-4), given as {@code employmentConfirmed}, which a local member doesn't need. The person
     * must belong to no provider yet (HLP-10), as {@code affiliation} says; the provider's own owner, who isn't on its
     * team yet, is the one exception (PRV-6).
     *
     * <p>Slice 6 adds PRV-8 here: a foreign member needs a Business provider.
     *
     * @throws PersonAlreadyBelongsToProviderException when the person owns another provider or is on a team already
     */
    public TeamMember addTeamMember(TeamMemberId memberId, TeamRole role, VerifiedPersonFacts person,
            boolean employmentConfirmed, Affiliation affiliation, Instant addedAt) {
        affiliation.admitJoining(id, person.personId());
        return TeamMember.add(memberId, id, role, person, employmentConfirmed, addedAt);
    }

    /**
     * The owner also works in their own team as a helper or supervisor (PRV-6). It is the same person, so HLP-10
     * allows it, once. They need what any local member needs (HLP-1, HLP-2) and no confirmation of lawful work, as the
     * owner is local (PRV-1).
     *
     * @throws IllegalArgumentException                when {@code owner} isn't this provider's owner
     * @throws PersonAlreadyBelongsToProviderException when the owner is on its team already
     */
    public TeamMember addOwnerToTeam(TeamMemberId memberId, TeamRole role, VerifiedPersonFacts owner,
            Affiliation affiliation, Instant addedAt) {
        if (!isRunBy(owner.personId())) {
            throw new IllegalArgumentException("Person " + owner.personId().value() + " doesn't run provider "
                    + id.value() + ", so can't join its team as its owner (PRV-6)");
        }
        return addTeamMember(memberId, role, owner, false, affiliation, addedAt);
    }

    /**
     * Payment reports that a payout account in the provider's name passed the holder check, held by the owner or by
     * the business ({@code PayoutAccountVerifiedIE}). The provider can now go live and be booked (PRV-2).
     */
    public void recordPayoutAccountVerified(PayoutAccountHolder holder) {
        this.verifiedPayoutAccount = Objects.requireNonNull(holder, "holder");
    }

    /**
     * Payment reports that the provider no longer has a verified payout account in its name
     * ({@code PayoutAccountVerifiedIE} with {@code verified} false). It can't be booked until one is verified (PRV-2).
     */
    public void recordPayoutAccountNotVerified() {
        this.verifiedPayoutAccount = null;
    }

    /**
     * Goes live with a verified payout account in the provider's name and at least one active helper (PRV-2).
     * Supervisors are optional and don't count (PRV-3); count with {@link Team#activeHelperCount()}.
     */
    public void goLive(int activeHelperCount) {
        if (status == ProviderStatus.LIVE) {
            throw new ProviderAlreadyLiveException(id.value());
        }
        if (verifiedPayoutAccount == null) {
            throw new NoVerifiedPayoutAccountException(id.value());
        }
        if (activeHelperCount < ProviderValues.MINIMUM_ACTIVE_HELPERS) {
            throw new NoActiveHelperException(id.value());
        }
        status = ProviderStatus.LIVE;
    }

    /**
     * Whether customers can book this provider (PRV-2): only when live, with a verified payout account in its name and
     * at least one active helper. A missing payout account is named before missing helpers, since only the owner can
     * fix it.
     */
    public Bookability bookabilityFor(int activeHelperCount) {
        if (status != ProviderStatus.LIVE) {
            return Bookability.NOT_LIVE;
        }
        if (verifiedPayoutAccount == null) {
            return Bookability.NO_VERIFIED_PAYOUT_ACCOUNT;
        }
        return activeHelperCount >= ProviderValues.MINIMUM_ACTIVE_HELPERS
                ? Bookability.BOOKABLE
                : Bookability.NO_ACTIVE_HELPERS;
    }

    public ProviderId id() {
        return id;
    }

    /** The one person who runs the provider (PRV-1). */
    public Owner owner() {
        return owner;
    }

    /**
     * True when {@code person} is the provider's owner, the one person who runs the provider account (PRV-1). Members
     * act for it only on the reservations they're rostered on (PRV-11), so they don't run its account.
     */
    public boolean isRunBy(PersonId person) {
        return owner.personId().equals(person);
    }

    public ProviderStatus status() {
        return status;
    }

    public boolean isLive() {
        return status == ProviderStatus.LIVE;
    }

    /** Who holds the provider's verified payout account; empty while it has none (PRV-2). */
    public Optional<PayoutAccountHolder> verifiedPayoutAccount() {
        return Optional.ofNullable(verifiedPayoutAccount);
    }
}
