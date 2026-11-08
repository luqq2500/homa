package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.ViewShelvesRequest;
import catalogue.usecase.dto.ViewShelvesResponse;
import catalogue.usecase.port.in.CustomerShelvesViewer;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.Comparator;
import java.util.stream.Collectors;

/** Shows the homepage shelves in the order Homa placed them. */
public final class CustomerViewShelves implements CustomerShelvesViewer {

    private final ShelfRepository shelves;

    public CustomerViewShelves(ShelfRepository shelves) {
        this.shelves = shelves;
    }

    @Override
    public ViewShelvesResponse view(ViewShelvesRequest request) {
        return new ViewShelvesResponse(shelves.all().stream()
                .sorted(Comparator.comparingInt(Shelf::order))
                .map(shelf -> new ViewShelvesResponse.Shelf(
                        shelf.id(),
                        shelf.title(),
                        shelf.order(),
                        shelf.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                        shelf.preset().hours(),
                        shelf.preset().helpers()))
                .toList());
    }
}
