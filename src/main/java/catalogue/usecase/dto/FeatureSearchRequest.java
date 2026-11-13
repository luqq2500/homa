package catalogue.usecase.dto;

import java.util.Set;

public record FeatureSearchRequest(String adminId, String name, Set<String> jobIds, int hours, int helpers) {

    public FeatureSearchRequest {
        jobIds = Set.copyOf(jobIds);
    }
}
