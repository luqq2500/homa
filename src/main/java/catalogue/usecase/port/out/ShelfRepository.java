package catalogue.usecase.port.out;

import catalogue.domain.Shelf;
import java.util.List;

/** Where the homepage shelves are kept. */
public interface ShelfRepository {

    void save(Shelf shelf);

    List<Shelf> all();
}
