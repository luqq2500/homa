package com.homa.reservation.domain.replica;

import com.homa.kernel.BusinessCalendar;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.roster.RosterMember;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider's helper or supervisor as Reservation sees it, kept up to date from Provider's team member events.
 *
 * @param active          verified, not suspended and not deactivated (§1 "Active", ROS-3)
 * @param rosterableUntil the last Kuala Lumpur date a foreign member's job may end on, which Provider computes from
 *                        the work permit expiry and its own 90-day buffer (HLP-3, ROS-4); empty for a citizen, who
 *                        needs no permit. Reservation never holds the 90 days itself (§11)
 */
public record RosterableMember(
        String memberId,
        String providerId,
        MemberRole role,
        String name,
        String photoReference,
        boolean active,
        Optional<LocalDate> rosterableUntil) {

    public RosterableMember {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(photoReference, "photoReference");
        Objects.requireNonNull(rosterableUntil, "rosterableUntil");
    }

    public static RosterableMember citizen(
            String memberId, String providerId, MemberRole role, String name, String photoReference, boolean active) {
        return new RosterableMember(memberId, providerId, role, name, photoReference, active, Optional.empty());
    }

    public static RosterableMember foreign(String memberId, String providerId, MemberRole role, String name,
            String photoReference, boolean active, LocalDate rosterableUntil) {
        return new RosterableMember(memberId, providerId, role, name, photoReference, active,
                Optional.of(rosterableUntil));
    }

    /**
     * ROS-3: only an active member can be rostered. ROS-4: a foreign member only on a job whose end date, on the
     * Kuala Lumpur calendar, is no later than {@code rosterableUntil}.
     */
    public boolean canWorkOn(Instant jobEnd) {
        LocalDate jobEndDate = BusinessCalendar.dateOf(jobEnd);
        return active && rosterableUntil.map(lastDate -> !jobEndDate.isAfter(lastDate)).orElse(true);
    }

    public boolean worksFor(String provider) {
        return providerId.equals(provider);
    }

    public RosterMember asRosterMember() {
        return new RosterMember(memberId, role, name, photoReference);
    }
}
