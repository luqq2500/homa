package catalogue.domain;

import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfJobNotOfferedException;
import catalogue.domain.exception.ShelfTitleRequiredException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A homepage shelf curated by Homa, such as "Weekend quick refresh", driven by a preset search.
 *
 * <p>A shelf searches only for jobs providers can offer. When Homa retires one of its jobs, the job comes
 * off the shelf's search; a shelf left with no job is no longer shown.
 */
public final class Shelf {

    private final String id;
    private final String title;
    private final int order;
    private PresetSearch preset;
    private boolean shown = true;

    private Shelf(String id, String title, PresetSearch preset, int order) {
        this.id = id;
        this.title = title;
        this.preset = preset;
        this.order = order;
    }

    /**
     * @param order the shelf's place on the homepage, from 1 for the first shelf
     * @param offeredJobs the jobs providers can offer now; the preset search may name no other
     */
    public static Shelf create(String title, PresetSearch preset, int order, Set<JobCode> offeredJobs) {
        if (title == null || title.isBlank()) {
            throw new ShelfTitleRequiredException();
        }
        if (order < 1) {
            throw new InvalidShelfOrderException(order);
        }
        preset.jobs().stream()
                .filter(job -> !offeredJobs.contains(job))
                .sorted((a, b) -> a.value().compareTo(b.value()))
                .findFirst()
                .ifPresent(job -> {
                    throw new ShelfJobNotOfferedException(job.value());
                });
        return new Shelf(new ShelfId().generate(), title.strip(), preset, order);
    }

    /**
     * Homa retired {@code retired}: it comes off the shelf's search. A shelf left with no job is no longer
     * shown, and keeps its last search.
     *
     * @return whether the shelf searched for the job, so it changed
     */
    public boolean withdrawJob(JobCode retired) {
        if (!preset.jobs().contains(retired)) {
            return false;
        }
        Set<JobCode> remaining = preset.jobs().stream()
                .filter(job -> !job.equals(retired))
                .collect(Collectors.toUnmodifiableSet());
        if (remaining.isEmpty()) {
            shown = false;
        } else {
            preset = PresetSearch.of(remaining, preset.hours(), preset.helpers());
        }
        return true;
    }

    /** Whether customers see the shelf: it still searches for at least one job providers can offer. */
    public boolean isShown() {
        return shown;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public PresetSearch preset() {
        return preset;
    }

    public int order() {
        return order;
    }
}
