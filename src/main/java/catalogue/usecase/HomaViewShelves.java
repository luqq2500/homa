package catalogue.usecase;

import catalogue.domain.Homepage;
import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.HomaViewShelvesResponse;
import catalogue.usecase.port.in.HomaShelvesViewer;
import catalogue.usecase.port.out.HomepageRepository;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Shows the founders every shelf in its place, including those no longer shown, so they can replace them. */
public final class HomaViewShelves implements HomaShelvesViewer {

    private final ShelfRepository shelves;
    private final HomepageRepository homepages;

    public HomaViewShelves(ShelfRepository shelves, HomepageRepository homepages) {
        this.shelves = shelves;
        this.homepages = homepages;
    }

    @Override
    public HomaViewShelvesResponse view() {
        List<String> order = homepages.find().map(Homepage::shelfIds).orElse(List.of());
        return new HomaViewShelvesResponse(order.stream()
                .map(shelves::find)
                .flatMap(Optional::stream)
                .map(shelf -> toView(shelf, order.indexOf(shelf.id()) + 1))
                .toList());
    }

    private static HomaViewShelvesResponse.Shelf toView(Shelf shelf, int position) {
        return new HomaViewShelvesResponse.Shelf(
                shelf.id(),
                shelf.title(),
                position,
                shelf.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                shelf.preset().hours(),
                shelf.preset().helpers(),
                shelf.isShown());
    }
}
