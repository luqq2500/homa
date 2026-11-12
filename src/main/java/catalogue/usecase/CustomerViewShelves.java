package catalogue.usecase;

import catalogue.domain.Homepage;
import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.CustomerViewShelvesResponse;
import catalogue.usecase.port.in.CustomerShelvesViewer;
import catalogue.usecase.port.out.HomepageRepository;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Shows a customer the homepage shelves, in the order Homa set. A shelf left with no job is not shown. */
public final class CustomerViewShelves implements CustomerShelvesViewer {

    private final ShelfRepository shelves;
    private final HomepageRepository homepages;

    public CustomerViewShelves(ShelfRepository shelves, HomepageRepository homepages) {
        this.shelves = shelves;
        this.homepages = homepages;
    }

    @Override
    public CustomerViewShelvesResponse view() {
        List<CustomerViewShelvesResponse.Shelf> inOrder = homepages.find().map(Homepage::shelfIds).orElse(List.of()).stream()
                .map(shelves::find)
                .flatMap(Optional::stream)
                .filter(Shelf::isShown)
                .map(CustomerViewShelves::toView)
                .toList();
        return new CustomerViewShelvesResponse(inOrder);
    }

    private static CustomerViewShelvesResponse.Shelf toView(Shelf shelf) {
        return new CustomerViewShelvesResponse.Shelf(
                shelf.id(),
                shelf.title(),
                shelf.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                shelf.preset().hours(),
                shelf.preset().helpers());
    }
}
