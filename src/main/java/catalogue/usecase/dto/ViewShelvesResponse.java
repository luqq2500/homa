package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

/** The homepage shelves in their order, each with the search it runs. */
public record ViewShelvesResponse(List<Shelf> shelves) {

    public ViewShelvesResponse {
        shelves = List.copyOf(shelves);
    }

    public record Shelf(String id, String title, int order, Set<String> jobCodes, int hours, int helpers) {
        public Shelf {
            jobCodes = Set.copyOf(jobCodes);
        }
    }
}
