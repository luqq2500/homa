package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.HomaViewShelvesResponse;
import catalogue.usecase.port.in.HomaShelvesViewer;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.Comparator;
import java.util.stream.Collectors;

/** Shows the founders every shelf in its order, including those no longer shown, so they can replace them. */
public final class HomaViewShelves implements HomaShelvesViewer {

    private final ShelfRepository shelves;

    public HomaViewShelves(ShelfRepository shelves) {
        this.shelves = shelves;
    }

    @Override
    public HomaViewShelvesResponse view() {
        return new HomaViewShelvesResponse(shelves.findAll().stream()
                .sorted(Comparator.comparingInt(Shelf::order))
                .map(HomaViewShelves::toView)
                .toList());
    }

    private static HomaViewShelvesResponse.Shelf toView(Shelf shelf) {
        return new HomaViewShelvesResponse.Shelf(
                shelf.id(),
                shelf.title(),
                shelf.order(),
                shelf.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                shelf.preset().hours(),
                shelf.preset().helpers(),
                shelf.isShown());
    }
}
