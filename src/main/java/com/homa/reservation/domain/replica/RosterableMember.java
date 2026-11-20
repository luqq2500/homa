package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.roster.RosterMember;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A provider's helper or supervisor as Reservation sees it, kept up to date from Provider's team member events.
 *
 * @param active          verified, not suspended and not deactivated (§1 "Active", ROS-3)
 * @param permitExpiry    a foreign member's work permit expiry date; {@link LocalDate#MAX} for a citizen, who
 *                        needs no permit
 */
public record RosterableMember(
        String memberId,
        String providerId,
        MemberRole role,
        String name,
        String photoReference,
        boolean active,
        LocalDate permitExpiry) {

    /** ROS-4: a foreign member's job must end at least this many calendar days before the permit expires. */
    private static final long PERMIT_MARGIN_DAYS = 90;

    public RosterableMember {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(photoReference, "photoReference");
        Objects.requireNonNull(permitExpiry, "permitExpiry");
    }

    public static RosterableMember citizen(
            String memberId, String providerId, MemberRole role, String name, String photoReference, boolean active) {
        return new RosterableMember(memberId, providerId, role, name, photoReference, active, LocalDate.MAX);
    }

    /**
     * ROS-3: only an active member can be rostered. ROS-4: on a job whose end date (Kuala Lumpur calendar) is no
     * later than the permit expiry date minus 90 days.
     */
    public boolean canWorkOn(Instant jobEnd) {
        return active && !BusinessCalendar.dateOf(jobEnd).isAfter(permitExpiry.minusDays(PERMIT_MARGIN_DAYS));
    }

    public boolean worksFor(String provider) {
        return providerId.equals(provider);
    }

    public RosterMember asRosterMember() {
        return new RosterMember(memberId, role, name, photoReference);
    }
}
