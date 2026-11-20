package com.homa.provider.domain;

import com.homa.provider.domain.exception.BusinessVerificationAlreadyRecordedException;
import com.homa.provider.domain.exception.NoActiveHelperException;
import com.homa.provider.domain.exception.ProviderAlreadyLiveException;
import com.homa.provider.domain.exception.ProviderNotVerifiedException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A homecare business on Homa. It registers with its business registration, continues onboarding once the business
 * is verified (PRV-1), and goes live with at least one active helper (PRV-2); supervisors are optional (PRV-3).
 *
 * <p>Left for slice 6: {@code register} refusing a registration with debt (PAY-9, PAY-11), {@code startClosing} and
 * {@code close} (PRV-5), suspension and re-verification (MON-1 to MON-4), and insurance (PRV-4).
 */
public final class Provider {

    /** PRV-2: a provider needs at least this many active helpers to go live. */
    public static final int MINIMUM_ACTIVE_HELPERS = 1;

    private final ProviderId id;
    private final BusinessRegistration registration;
    private ProviderStatus status;
    private Instant businessVerifiedAt;

    private Provider(ProviderId id, BusinessRegistration registration, ProviderStatus status,
            Instant businessVerifiedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.registration = Objects.requireNonNull(registration, "registration");
        this.status = Objects.requireNonNull(status, "status");
        this.businessVerifiedAt = businessVerifiedAt;
    }

    /** A business submits its registration and awaits business verification (PRV-1). */
    public static Provider register(ProviderId id, BusinessRegistration registration) {
        return new Provider(id, registration, ProviderStatus.REGISTERED, null);
    }

    /** Rebuilds a provider from what was stored; {@code businessVerifiedAt} is null until the business is verified. */
    public static Provider restore(ProviderId id, BusinessRegistration registration, ProviderStatus status,
            Instant businessVerifiedAt) {
        return new Provider(id, registration, status, businessVerifiedAt);
    }

    /**
     * Accepts the result of the business verification (KYB) that the registration is a legal business entity in the
     * country (PRV-1). A pass lets the provider continue onboarding; a failure stops it.
     */
    public void recordBusinessVerification(VerificationOutcome outcome, Instant verifiedAt) {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(verifiedAt, "verifiedAt");
        if (status != ProviderStatus.REGISTERED) {
            throw new BusinessVerificationAlreadyRecordedException(id.value());
        }
        if (outcome.passed()) {
            status = ProviderStatus.VERIFIED;
            businessVerifiedAt = verifiedAt;
        } else {
            status = ProviderStatus.VERIFICATION_FAILED;
        }
    }

    /**
     * Adds a helper or supervisor who passed verification (HLP-1 to HLP-4). Only a verified business can build its
     * team (PRV-1), before or after going live (PRV-3: a supervisor can join later).
     */
    public TeamMember addTeamMember(TeamMemberId memberId, TeamRole role, Eligibility eligibility,
            MemberVerification verification) {
        requireBusinessVerified();
        return TeamMember.add(memberId, id, role, eligibility, verification);
    }

    /**
     * Goes live once the business is verified (PRV-1) and with at least one active helper (PRV-2). Supervisors are
     * optional and don't count (PRV-3); count with {@link Team#activeHelperCount()}.
     */
    public void goLive(int activeHelperCount) {
        if (status == ProviderStatus.LIVE) {
            throw new ProviderAlreadyLiveException(id.value());
        }
        requireBusinessVerified();
        if (activeHelperCount < MINIMUM_ACTIVE_HELPERS) {
            throw new NoActiveHelperException(id.value());
        }
        status = ProviderStatus.LIVE;
    }

    /** Whether customers can book this provider: only when live and with an active helper (PRV-6). */
    public Bookability bookabilityFor(int activeHelperCount) {
        if (status != ProviderStatus.LIVE) {
            return Bookability.NOT_LIVE;
        }
        return activeHelperCount >= MINIMUM_ACTIVE_HELPERS ? Bookability.BOOKABLE : Bookability.NO_ACTIVE_HELPERS;
    }

    private void requireBusinessVerified() {
        if (!status.isBusinessVerified()) {
            throw new ProviderNotVerifiedException(id.value());
        }
    }

    public ProviderId id() {
        return id;
    }

    public BusinessRegistration registration() {
        return registration;
    }

    public ProviderStatus status() {
        return status;
    }

    public boolean isLive() {
        return status == ProviderStatus.LIVE;
    }

    /** When the business passed verification, if it has. */
    public Optional<Instant> businessVerifiedAt() {
        return Optional.ofNullable(businessVerifiedAt);
    }
}
