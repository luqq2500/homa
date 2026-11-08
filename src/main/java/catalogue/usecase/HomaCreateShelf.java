package catalogue.usecase;

import catalogue.domain.JobCode;
import catalogue.domain.PresetSearch;
import catalogue.domain.Shelf;
import catalogue.usecase.dto.CreateShelfRequest;
import catalogue.usecase.dto.CreateShelfResponse;
import catalogue.usecase.port.in.HomaShelfCreator;
import catalogue.usecase.port.out.ShelfRepository;
import java.util.stream.Collectors;

/** Creates a homepage shelf driven by a preset search. */
public final class HomaCreateShelf implements HomaShelfCreator {

    private final ShelfRepository shelves;

    public HomaCreateShelf(ShelfRepository shelves) {
        this.shelves = shelves;
    }

    @Override
    public CreateShelfResponse create(CreateShelfRequest request) {
        PresetSearch preset = PresetSearch.of(
                request.jobCodes().stream().map(JobCode::of).collect(Collectors.toSet()), request.hours(), request.helpers());
        Shelf shelf = Shelf.create(request.title(), preset, request.order());
        shelves.save(shelf);
        return new CreateShelfResponse(shelf.id(), shelf.title(), shelf.order());
    }
}
