package offering.usecase.dto;

import java.util.Set;

public record SetToolsRequest(String providerId, Set<String> toolIds) {

    public SetToolsRequest {
        toolIds = Set.copyOf(toolIds);
    }
}
