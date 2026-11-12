package catalogue.usecase.port.out;

import catalogue.domain.Shelf;
import java.util.List;

/** Keeps the homepage shelves. */
public interface ShelfRepository {

    void save(Shelf shelf);

    /** Every shelf, in no particular order. */
    List<Shelf> findAll();
}
