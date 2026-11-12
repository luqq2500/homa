package catalogue.usecase.port.out;

import catalogue.domain.Homepage;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the order of the shelves on the homepage.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface HomepageRepository {

    /** The homepage, or empty before the first shelf is created. */
    Optional<Homepage> find();

    void save(Homepage homepage);
}
