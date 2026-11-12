package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

/** The homepage shelves, in the order they are shown. */
public record CustomerViewShelvesResponse(List<Shelf> shelves) {

    public CustomerViewShelvesResponse {
        shelves = List.copyOf(shelves);
    }

    /** One shelf and the search it runs. */
    public record Shelf(String shelfId, String title, Set<String> jobCodes, int hours, int helpers) {

        public Shelf {
            jobCodes = Set.copyOf(jobCodes);
        }
    }
}
