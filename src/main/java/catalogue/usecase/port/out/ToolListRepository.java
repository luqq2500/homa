package catalogue.usecase.port.out;

import catalogue.domain.ToolList;
import catalogue.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps Homa's one tool list.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found, for example because another founder saved first. The founder then tries again on what is
 * there now; nothing is overwritten.
 */
public interface ToolListRepository {

    /** The tool list, or empty before the first tool is added. */
    Optional<ToolList> find();

    void save(ToolList toolList);
}
