package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.CustomerViewShelvesResponse;
import catalogue.usecase.port.in.CustomerShelvesViewer;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Shows a customer the homepage shelves, in the order Homa set. */
public final class CustomerViewShelves implements CustomerShelvesViewer {

    private final ShelfRepository shelves;

    public CustomerViewShelves(ShelfRepository shelves) {
        this.shelves = shelves;
    }

    @Override
    public CustomerViewShelvesResponse view() {
        List<CustomerViewShelvesResponse.Shelf> inOrder = shelves.findAll().stream()
                .sorted(Comparator.comparingInt(Shelf::order))
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
