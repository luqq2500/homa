package catalogue.usecase.dto;

import java.util.List;
import java.util.Set;

/** Every homepage shelf in its order, and whether customers still see it. */
public record HomaViewShelvesResponse(List<Shelf> shelves) {

    public HomaViewShelvesResponse {
        shelves = List.copyOf(shelves);
    }

    /** One shelf, its search, and whether it is shown: a shelf left with no job is not. */
    public record Shelf(String shelfId, String title, int order, Set<String> jobCodes, int hours, int helpers, boolean shown) {

        public Shelf {
            jobCodes = Set.copyOf(jobCodes);
        }
    }
}
