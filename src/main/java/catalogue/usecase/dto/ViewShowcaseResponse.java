package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

public record ViewShowcaseResponse(List<Search> searches) {

    public ViewShowcaseResponse {
        searches = List.copyOf(searches);
    }

    public record Search(String searchId, String name, Set<String> jobIds, int hours, int helpers) {

        public Search {
            jobIds = Set.copyOf(jobIds);
        }
    }
}
