package com.homa.reservation.domain.replica;

import com.homa.kernel.BusinessCalendar;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.roster.RosterMember;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider's helper or supervisor as Reservation sees it, kept up to date from Provider's team member events and
 * the member's verified profile photo.
 *
 * @param photoVerified   the member's face photo is verified against their identity (IDN-4); only then can they be
 *                        rostered, because the customer sees it on the roster (ROS-6)
 * @param photoReference  the member's verified photo in the photo store; a new photo replaces it only once it is
 *                        verified (IDN-4). Empty until Reservation has heard which photo it is
 * @param active          verified, not suspended and not deactivated (§1 "Active", ROS-3)
 * @param rosterableUntil the last Kuala Lumpur date a foreign member's job may end on, which Provider computes from
 *                        the work permit expiry and its own 90-day buffer (HLP-3, ROS-4); empty for a local member,
 *                        who needs no permit. Reservation never holds the 90 days itself (§13)
 */
public record RosterableMember(
        String memberId,
        String providerId,
        MemberRole role,
        String name,
        boolean photoVerified,
        Optional<String> photoReference,
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

    /** A local member (§1 "Local") with a verified photo. */
    public static RosterableMember local(String memberId, String providerId, MemberRole role, String name,
            String verifiedPhotoReference, boolean active) {
        return new RosterableMember(memberId, providerId, role, name, true, Optional.of(verifiedPhotoReference),
                active, Optional.empty());
    }

    /** A foreign member (§1 "Foreign") with a verified photo, rosterable on jobs ending by {@code rosterableUntil}. */
    public static RosterableMember foreign(String memberId, String providerId, MemberRole role, String name,
            String verifiedPhotoReference, boolean active, LocalDate rosterableUntil) {
        return new RosterableMember(memberId, providerId, role, name, true, Optional.of(verifiedPhotoReference),
                active, Optional.of(rosterableUntil));
    }

    /** IDN-4: the member's new photo is verified, and replaces the one shown on the roster. */
    public RosterableMember withVerifiedPhoto(String verifiedPhotoReference) {
        return new RosterableMember(memberId, providerId, role, name, true, Optional.of(verifiedPhotoReference),
                active, rosterableUntil);
    }

    /** IDN-4: the member shows the person's verified photo once Verification has verified one; else nothing changes. */
    public RosterableMember showing(PersonPhoto photo) {
        return photo.verifiedReference().map(this::withVerifiedPhoto).orElse(this);
    }

    /** IDN-4, ROS-6: the member has a verified photo the customer can be shown. */
    public boolean hasVerifiedPhoto() {
        return photoVerified && photoReference.isPresent();
    }

    /**
     * ROS-3: only an active member can be rostered. IDN-4, ROS-6: only with a verified photo. ROS-4: a foreign
     * member only on a job whose end date, on the Kuala Lumpur calendar, is no later than {@code rosterableUntil}.
     */
    public boolean canWorkOn(Instant jobEnd) {
        LocalDate jobEndDate = BusinessCalendar.dateOf(jobEnd);
        return active && hasVerifiedPhoto()
                && rosterableUntil.map(lastDate -> !jobEndDate.isAfter(lastDate)).orElse(true);
    }

    public boolean worksFor(String provider) {
        return providerId.equals(provider);
    }

    /** ROS-6: the member as the customer sees them on the roster, by name and verified photo. */
    public RosterMember asRosterMember() {
        if (!hasVerifiedPhoto()) {
            throw new MemberNotRosterableException(memberId);
        }
        return new RosterMember(memberId, role, name, photoReference.orElseThrow());
    }
}
