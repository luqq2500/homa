package com.homa.provider.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The work permit buffer (HLP-3, ROS-4). A foreign member must have at least {@code minimumDaysLeft} calendar days
 * left on their work permit when added, counted on the Kuala Lumpur calendar from the moment they are added: a permit
 * expiring exactly 90 days later is enough, one expiring 89 days later is not. Once added, they can be rostered only
 * on jobs ending at least that many days before the permit expires.
 */
public record PermitRequirement(int minimumDaysLeft) {

    /** The buffer for every foreign member: 90 days. */
    public static final PermitRequirement FOREIGN_TEAM_MEMBER = new PermitRequirement(ProviderValues.WORK_PERMIT_BUFFER_DAYS);

    /** True when a permit expiring on {@code permitExpiry} has enough days left at {@code addedAt} (HLP-3). */
    public boolean isMetBy(LocalDate permitExpiry, Instant addedAt) {
        Objects.requireNonNull(permitExpiry, "permitExpiry");
        LocalDate earliestAcceptedExpiry = BusinessCalendar.dateOf(BusinessCalendar.plusDays(addedAt, minimumDaysLeft));
        return !permitExpiry.isBefore(earliestAcceptedExpiry);
    }

    /**
     * The last Kuala Lumpur date a job the member is rostered on may end: the permit's expiry minus the buffer (ROS-4).
     * Published as {@code rosterableUntil}, so Reservation never holds the 90 days itself.
     */
    public LocalDate rosterableUntil(LocalDate permitExpiry) {
        return permitExpiry.minusDays(minimumDaysLeft);
    }
}
