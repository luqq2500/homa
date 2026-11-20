package com.homa.provider.domain;

import com.homa.provider.domain.exception.EmploymentNotConfirmedException;
import com.homa.provider.domain.exception.WorkPermitExpiresTooSoonException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Why a helper or supervisor may work in the country: as a citizen (HLP-2), or as a foreign worker with a work
 * permit (HLP-3) whom the provider confirms it lawfully employs (HLP-4).
 */
public sealed interface Eligibility {

    /** True for a citizen, false for a foreign worker. */
    boolean isCitizen();

    /**
     * Refuses a member who isn't allowed to work for the provider at the moment they are verified.
     *
     * @throws WorkPermitExpiresTooSoonException when a foreign worker's permit has under 90 days left (HLP-3)
     * @throws EmploymentNotConfirmedException when the provider hasn't confirmed a foreign worker's employment (HLP-4)
     */
    void requireAllowedToWorkAt(Instant verifiedAt, TeamMemberId memberId);

    /** Verified as a citizen of the country (HLP-2). */
    record Citizen() implements Eligibility {

        @Override
        public boolean isCitizen() {
            return true;
        }

        @Override
        public void requireAllowedToWorkAt(Instant verifiedAt, TeamMemberId memberId) {
            // HLP-2 is met by the identity verification itself.
        }
    }

    /**
     * Verified as legally allowed to work in the country until {@code permitExpiry} (HLP-3), with the provider's
     * confirmation that it lawfully employs the worker under its registered business (HLP-4).
     */
    record ForeignWorker(LocalDate permitExpiry, boolean employmentConfirmed) implements Eligibility {

        public ForeignWorker {
            Objects.requireNonNull(permitExpiry, "permitExpiry");
        }

        @Override
        public boolean isCitizen() {
            return false;
        }

        @Override
        public void requireAllowedToWorkAt(Instant verifiedAt, TeamMemberId memberId) {
            if (!employmentConfirmed) {
                throw new EmploymentNotConfirmedException(memberId.value());
            }
            if (!PermitRequirement.FOREIGN_TEAM_MEMBER.isMetBy(permitExpiry, verifiedAt)) {
                throw new WorkPermitExpiresTooSoonException(memberId.value());
            }
        }
    }
}
