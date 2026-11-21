package com.homa.provider.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * HLP-1: every helper and supervisor must be aged {@code minimumAge} or above, on the Kuala Lumpur date they are
 * verified. A candidate is of age from their birthday onwards; one born on 29 February comes of age on 1 March in a
 * year without 29 February, never earlier.
 */
public record AgeRequirement(int minimumAge) {

    private static final int MONTHS_PER_YEAR = 12;

    /** The requirement for every helper and supervisor: 18 years. */
    public static final AgeRequirement TEAM_MEMBER = new AgeRequirement(ProviderValues.MINIMUM_HELPER_AGE_YEARS);

    /** True when someone born on {@code dateOfBirth} is at least {@link #minimumAge()} at {@code verifiedAt}. */
    public boolean isMetBy(LocalDate dateOfBirth, Instant verifiedAt) {
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        long months = (long) minimumAge * MONTHS_PER_YEAR;
        LocalDate latestQualifyingBirthDate = BusinessCalendar.dateOf(BusinessCalendar.plusMonths(verifiedAt, -months));
        return !dateOfBirth.isAfter(latestQualifyingBirthDate);
    }
}
