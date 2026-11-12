package catalogue.usecase.dto;

import java.util.Set;

/**
 * What a founder gives to feature a search: its title, the preset search it fills in, and its place in the
 * showcase, from 1.
 */
public record FeatureSearchRequest(String decidedBy, String title, Set<String> jobCodes, int hours, int helpers, int position) {

    public FeatureSearchRequest {
        jobCodes = Set.copyOf(jobCodes);
    }
}
