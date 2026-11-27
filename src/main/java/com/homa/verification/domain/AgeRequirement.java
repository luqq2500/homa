package com.homa.verification.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CUS-10, PRV-1, HLP-1: every customer, owner and member must be aged {@code minimumAge} or above, on the Kuala
 * Lumpur date they are checked. A person is of age from their birthday onwards; one born on 29 February comes of
 * age on 1 March in a year without 29 February, never earlier.
 */
public record AgeRequirement(int minimumAge) {

    private static final int MONTHS_PER_YEAR = 12;

    /** The requirement for every customer, owner and member: 18 years. */
    public static final AgeRequirement ADULT = new AgeRequirement(VerificationValues.MINIMUM_AGE_YEARS);

    /** True when someone born on {@code dateOfBirth} is at least {@link #minimumAge()} at {@code checkedAt}. */
    public boolean isMetBy(LocalDate dateOfBirth, Instant checkedAt) {
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        long months = (long) minimumAge * MONTHS_PER_YEAR;
        LocalDate latestQualifyingBirthDate = BusinessCalendar.dateOf(BusinessCalendar.plusMonths(checkedAt, -months));
        return !dateOfBirth.isAfter(latestQualifyingBirthDate);
    }
}
