package com.homa.provider.domain;

import com.homa.provider.domain.exception.EmploymentNotConfirmedException;
import com.homa.provider.domain.exception.WorkPermitExpiresTooSoonException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Why a member may work in the country: as a local, verified with the country's local document (HLP-2), or as a
 * foreign worker with a work permit (HLP-3) whom the provider confirms works lawfully for it (HLP-4). Whether a person
 * is local or foreign is Verification's decision (§14); Provider takes it from {@link VerifiedPersonFacts}.
 *
 * <p>Left for slice 6: only a Business provider can have a {@link ForeignWorker} (PRV-8). Until Business status
 * exists, a foreign member can join any provider, so Kemas Home can still add Rina in slice 1.
 */
public sealed interface Eligibility {

    /**
     * The eligibility a verified person has as a member: local or foreign, as Verification decided.
     * {@code employmentConfirmed} is the provider's confirmation that a foreign member works lawfully for it (HLP-4);
     * a local member needs none, so it is ignored for one.
     */
    static Eligibility of(VerifiedPersonFacts person, boolean employmentConfirmed) {
        return person.local() ? new Local() : new ForeignWorker(person.permitExpiry(), employmentConfirmed);
    }

    /** True for a local member, false for a foreign worker. */
    boolean isLocal();

    /**
     * Refuses a member who isn't allowed to work for the provider at the moment they are added.
     *
     * @throws EmploymentNotConfirmedException when the provider hasn't confirmed a foreign member works lawfully for
     *                                         it (HLP-4)
     * @throws WorkPermitExpiresTooSoonException when a foreign member's permit has under 90 days left (HLP-3)
     */
    void requireAllowedToWorkAt(Instant addedAt, TeamMemberId memberId);

    /**
     * The last Kuala Lumpur date a rostered job may end on: the work permit's expiry minus the 90-day buffer (ROS-4).
     * Empty for a local member, who has no such limit.
     */
    Optional<LocalDate> rosterableUntil();

    /** Verified as local, with the document the country policy sets for locals (HLP-2, IDN-1). */
    record Local() implements Eligibility {

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public void requireAllowedToWorkAt(Instant addedAt, TeamMemberId memberId) {
            // HLP-2 is met by the identity verification itself.
        }

        @Override
        public Optional<LocalDate> rosterableUntil() {
            return Optional.empty();
        }
    }

    /**
     * Verified as foreign and allowed to work in the country until {@code permitExpiry} (HLP-3), with the provider's
     * confirmation that the member works lawfully for it (HLP-4).
     */
    record ForeignWorker(LocalDate permitExpiry, boolean employmentConfirmed) implements Eligibility {

        public ForeignWorker {
            Objects.requireNonNull(permitExpiry, "permitExpiry");
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public void requireAllowedToWorkAt(Instant addedAt, TeamMemberId memberId) {
            if (!employmentConfirmed) {
                throw new EmploymentNotConfirmedException(memberId.value());
            }
            if (!PermitRequirement.FOREIGN_TEAM_MEMBER.isMetBy(permitExpiry, addedAt)) {
                throw new WorkPermitExpiresTooSoonException(memberId.value());
            }
        }

        @Override
        public Optional<LocalDate> rosterableUntil() {
            return Optional.of(PermitRequirement.FOREIGN_TEAM_MEMBER.rosterableUntil(permitExpiry));
        }
    }
}
