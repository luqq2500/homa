package catalogue.domain;

import catalogue.domain.exception.FeaturedSearchInvalidException;
import java.util.HashSet;
import java.util.Set;

/** A search Homa features on the homepage: tapping it fills in the search with its jobs, duration and helpers. */
public record FeaturedSearch(String id, String name, Set<String> jobIds, int hours, int helpers) {

    public FeaturedSearch {
        name = Names.required(name);
        jobIds = Set.copyOf(jobIds);
        if (hours < 1) {
            throw new FeaturedSearchInvalidException("A featured search is for at least 1 hour");
        }
        if (helpers < 1) {
            throw new FeaturedSearchInvalidException("A featured search is for at least 1 helper");
        }
    }

    static FeaturedSearch feature(String name, Set<String> jobIds, int hours, int helpers) {
        if (jobIds.isEmpty()) {
            throw new FeaturedSearchInvalidException("A featured search searches for at least one job");
        }
        return new FeaturedSearch(new FeaturedSearchId().generate(), name, jobIds, hours, helpers);
    }

    FeaturedSearch renamed(String name) {
        return new FeaturedSearch(id, name, jobIds, hours, helpers);
    }

    FeaturedSearch without(String jobId) {
        Set<String> left = new HashSet<>(jobIds);
        left.remove(jobId);
        return new FeaturedSearch(id, name, left, hours, helpers);
    }

    /** A featured search left with no job is no longer shown. */
    public boolean isShown() {
        return !jobIds.isEmpty();
    }
}
