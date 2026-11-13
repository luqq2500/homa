package offering.usecase.dto;

import java.util.Set;

public record SetJobsRequest(String providerId, Set<String> jobIds) {

    public SetJobsRequest {
        jobIds = Set.copyOf(jobIds);
    }
}
