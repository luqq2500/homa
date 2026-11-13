package catalogue.domain;

import catalogue.domain.exception.FeaturedSearchNotFoundException;
import catalogue.domain.exception.PlaceOutsideShowcaseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The homepage showcase: the searches Homa features, each in its own place, in the order Homa sets. */
public final class Showcase {

    private final List<FeaturedSearch> searches;

    public Showcase(List<FeaturedSearch> searches) {
        this.searches = new ArrayList<>(searches);
    }

    public static Showcase empty() {
        return new Showcase(List.of());
    }

    /** Features a search for jobs providers can offer, in the last place. */
    public FeaturedSearch feature(String name, List<Job> jobs, int hours, int helpers) {
        jobs.forEach(Job::requireOfferable);
        Set<String> jobIds = Set.copyOf(jobs.stream().map(Job::id).toList());
        FeaturedSearch search = FeaturedSearch.feature(name, jobIds, hours, helpers);
        searches.add(search);
        return search;
    }

    public FeaturedSearch rename(String searchId, String name) {
        int index = indexOf(searchId);
        FeaturedSearch renamed = searches.get(index).renamed(name);
        searches.set(index, renamed);
        return renamed;
    }

    /** Moves a search to a place, counted from 1; the searches after it move along. */
    public void move(String searchId, int place) {
        int index = indexOf(searchId);
        if (place < 1 || place > searches.size()) {
            throw new PlaceOutsideShowcaseException(place);
        }
        searches.add(place - 1, searches.remove(index));
    }

    public void remove(String searchId) {
        searches.remove(indexOf(searchId));
    }

    /** A retired job comes off every featured search. */
    public void retireJob(String jobId) {
        searches.replaceAll(search -> search.without(jobId));
    }

    /** The place of a search, counted from 1. */
    public int placeOf(String searchId) {
        return indexOf(searchId) + 1;
    }

    /** Every featured search, in order, including those left with no job. */
    public List<FeaturedSearch> searches() {
        return List.copyOf(searches);
    }

    /** What customers see: the searches that still have a job, in order. */
    public List<FeaturedSearch> shown() {
        return searches.stream().filter(FeaturedSearch::isShown).toList();
    }

    private int indexOf(String searchId) {
        for (int index = 0; index < searches.size(); index++) {
            if (searches.get(index).id().equals(searchId)) {
                return index;
            }
        }
        throw new FeaturedSearchNotFoundException(searchId);
    }
}
