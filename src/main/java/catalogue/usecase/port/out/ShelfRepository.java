package catalogue.usecase.port.out;

import catalogue.domain.Shelf;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the homepage shelves. Where each one sits is the {@link HomepageRepository}'s.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface ShelfRepository {

    Optional<Shelf> find(String shelfId);

    /** Every shelf, in no particular order. */
    List<Shelf> findAll();

    void save(Shelf shelf);

    /** Deletes a shelf that is no longer on the homepage. */
    void remove(String shelfId);
}
