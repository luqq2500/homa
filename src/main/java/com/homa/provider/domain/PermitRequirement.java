package com.homa.provider.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * HLP-3: a foreign helper or supervisor must have at least {@code minimumDaysLeft} calendar days before their work
 * permit expires, counted on the Kuala Lumpur calendar from the day they are verified. A permit expiring exactly
 * 90 days after verification passes; one expiring 89 days after does not.
 */
public record PermitRequirement(int minimumDaysLeft) {

    /** The requirement for every foreign helper and supervisor: 90 days. */
    public static final PermitRequirement FOREIGN_TEAM_MEMBER = new PermitRequirement(ProviderValues.WORK_PERMIT_BUFFER_DAYS);

    /** True when a permit expiring on {@code permitExpiry} has enough days left at {@code verifiedAt}. */
    public boolean isMetBy(LocalDate permitExpiry, Instant verifiedAt) {
        Objects.requireNonNull(permitExpiry, "permitExpiry");
        LocalDate earliestAcceptedExpiry = BusinessCalendar.dateOf(BusinessCalendar.plusDays(verifiedAt, minimumDaysLeft));
        return !permitExpiry.isBefore(earliestAcceptedExpiry);
    }
}
