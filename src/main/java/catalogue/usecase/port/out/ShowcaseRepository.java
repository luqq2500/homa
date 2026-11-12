package catalogue.usecase.port.out;

import catalogue.domain.Showcase;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the homepage showcase: the featured searches and their order.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface ShowcaseRepository {

    /** The showcase, or empty before the first search is featured. */
    Optional<Showcase> find();

    void save(Showcase showcase);
}
