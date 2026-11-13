package catalogue.domain;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A search Homa features on the homepage, such as "Weekend quick refresh": a title and the preset search it
 * fills in. The customer still picks the date and time, and the city comes from their address.
 *
 * <p>A featured search searches only for jobs providers can offer. When Homa retires one of its jobs, the
 * job comes off it; one left with no job is no longer shown, and keeps its last search so the admins can
 * see what it was.
 *
 * <p>Featured searches change only through their {@link Showcase}, which also keeps their order.
 */
public final class FeaturedSearch {

    private final String id;
    private String title;
    private PresetSearch preset;
    private boolean shown = true;

    private FeaturedSearch(String id, String title, PresetSearch preset) {
        this.id = id;
        this.title = title;
        this.preset = preset;
    }

    static FeaturedSearch of(String title, PresetSearch preset) {
        return new FeaturedSearch(new FeaturedSearchId().generate(), CatalogueText.requireTitle(title), preset);
    }

    boolean rename(String newTitle) {
        String renamed = CatalogueText.requireTitle(newTitle);
        boolean changed = !renamed.equals(title);
        title = renamed;
        return changed;
    }

    boolean withdrawJob(JobCode retired) {
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

    /** Whether customers see it: it still searches for at least one job providers can offer. */
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
}
