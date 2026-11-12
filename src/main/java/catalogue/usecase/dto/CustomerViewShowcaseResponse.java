package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

/** The featured searches on the homepage, in the order they are shown. */
public record CustomerViewShowcaseResponse(List<FeaturedSearch> featuredSearches) {

    public CustomerViewShowcaseResponse {
        featuredSearches = List.copyOf(featuredSearches);
    }

    /**
     * One featured search and the search it fills in. The customer picks the date and time; the city comes
     * from their address.
     */
    public record FeaturedSearch(String featuredSearchId, String title, Set<String> jobCodes, int hours, int helpers) {

        public FeaturedSearch {
            jobCodes = Set.copyOf(jobCodes);
        }
    }
}
