package catalogue.domain;

import catalogue.domain.exception.FeaturedSearchJobNotOfferedException;
import catalogue.domain.exception.FeaturedSearchNotInShowcaseException;
import catalogue.domain.exception.InvalidShowcasePositionException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The homepage showcase: the searches Homa features, in the order customers see them. Positions run from 1,
 * the first, with no gaps and no two featured searches in one place: featuring, moving or removing one
 * moves the others along.
 */
public final class Showcase {

    private final String id;
    private final List<FeaturedSearch> featured = new ArrayList<>();

    private Showcase(String id) {
        this.id = id;
    }

    public static Showcase create() {
        return new Showcase(new ShowcaseId().generate());
    }

    /**
     * Features a new search at {@code position}, from 1 to one past the last; the ones from there move down
     * one.
     *
     * @param offeredJobs the jobs providers can offer now; the preset search may name no other
     */
    public FeaturedSearch feature(String title, PresetSearch preset, int position, Set<JobCode> offeredJobs) {
        Objects.requireNonNull(preset);
        preset.jobs().stream()
                .filter(job -> !offeredJobs.contains(job))
                .min(Comparator.comparing(JobCode::value))
                .ifPresent(job -> {
                    throw new FeaturedSearchJobNotOfferedException(job.value());
                });
        FeaturedSearch search = FeaturedSearch.of(title, preset);
        requirePosition(position, featured.size() + 1);
        featured.add(position - 1, search);
        return search;
    }

    /** Gives a featured search a new title. Returns whether it changed: the same title changes nothing. */
    public boolean rename(String featuredSearchId, String title) {
        return existing(featuredSearchId).rename(title);
    }

    /**
     * Moves a featured search to {@code position}, from 1 to the last; the ones in between move along.
     * Returns whether it moved: its own position changes nothing.
     */
    public boolean move(String featuredSearchId, int position) {
        FeaturedSearch search = existing(featuredSearchId);
        int from = featured.indexOf(search) + 1;
        requirePosition(position, featured.size());
        featured.remove(search);
        featured.add(position - 1, search);
        return from != position;
    }

    /** Takes a featured search off the showcase; the ones after it move up one. */
    public void remove(String featuredSearchId) {
        featured.remove(existing(featuredSearchId));
    }

    /**
     * Homa retired {@code retired}: it comes off every featured search that searches for it. Returns whether
     * any did, so the showcase changed.
     */
    public boolean withdrawJob(JobCode retired) {
        boolean changed = false;
        for (FeaturedSearch search : featured) {
            changed |= search.withdrawJob(retired);
        }
        return changed;
    }

    /** Every featured search, shown or not, in the order customers see them. */
    public List<FeaturedSearch> featuredSearches() {
        return List.copyOf(featured);
    }

    public Optional<FeaturedSearch> featuredSearch(String featuredSearchId) {
        return featured.stream().filter(search -> search.id().equals(featuredSearchId)).findFirst();
    }

    /** Where a featured search sits, from 1 for the first. */
    public int positionOf(String featuredSearchId) {
        return featured.indexOf(existing(featuredSearchId)) + 1;
    }

    public String id() {
        return id;
    }

    private FeaturedSearch existing(String featuredSearchId) {
        return featuredSearch(featuredSearchId)
                .orElseThrow(() -> new FeaturedSearchNotInShowcaseException(featuredSearchId));
    }

    private static void requirePosition(int position, int last) {
        if (position < 1 || position > last) {
            throw new InvalidShowcasePositionException(position, last);
        }
    }
}
