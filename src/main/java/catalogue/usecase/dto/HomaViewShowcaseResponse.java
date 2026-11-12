package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

/** Every featured search in its place, and whether customers still see it. */
public record HomaViewShowcaseResponse(List<FeaturedSearch> featuredSearches) {

    public HomaViewShowcaseResponse {
        featuredSearches = List.copyOf(featuredSearches);
    }

    /** One featured search, the search it fills in, and whether it is shown: one left with no job is not. */
    public record FeaturedSearch(
            String featuredSearchId, String title, int position, Set<String> jobCodes, int hours, int helpers, boolean shown) {

        public FeaturedSearch {
            jobCodes = Set.copyOf(jobCodes);
        }
    }
}
