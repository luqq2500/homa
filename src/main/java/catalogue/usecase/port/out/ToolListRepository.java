package catalogue.usecase.port.out;

import catalogue.domain.ToolList;
import java.util.Optional;

/** Keeps Homa's one tool list. */
public interface ToolListRepository {

    /** The tool list, or empty before the first tool is added. */
    Optional<ToolList> find();

    void save(ToolList toolList);
}
