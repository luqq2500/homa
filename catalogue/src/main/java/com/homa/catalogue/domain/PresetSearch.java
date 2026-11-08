package com.homa.catalogue.domain;

import com.homa.catalogue.domain.exception.EmptyPresetSearchException;
import com.homa.catalogue.domain.exception.PresetSearchOutOfRangeException;
import java.util.Objects;
import java.util.Set;

/**
 * The search a homepage shelf runs: the jobs, the duration in hours and the number of helpers.
 *
 * <p>It needs at least one job, one hour and one helper. Homa's upper limits (6 hours, 10 helpers) are
 * Homa-set values that can change, so they are checked when the search runs, not when the shelf is
 * made.
 */
public record PresetSearch(Set<JobCode> jobs, int hours, int helpers) {

    public PresetSearch {
        jobs = Set.copyOf(Objects.requireNonNull(jobs));
        if (jobs.isEmpty()) {
            throw new EmptyPresetSearchException();
        }
        if (hours < 1) {
            throw new PresetSearchOutOfRangeException("hour", hours);
        }
        if (helpers < 1) {
            throw new PresetSearchOutOfRangeException("helper", helpers);
        }
    }

    public static PresetSearch of(Set<JobCode> jobs, int hours, int helpers) {
        return new PresetSearch(jobs, hours, helpers);
    }
}
