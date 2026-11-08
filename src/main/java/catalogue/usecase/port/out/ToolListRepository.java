package catalogue.usecase.port.out;

import catalogue.domain.ToolList;
import java.util.Optional;

/** Where Homa's tool list is kept. There is one tool list. */
public interface ToolListRepository {

    /** The tool list, or empty before Homa adds the first tool. */
    Optional<ToolList> load();

    void save(ToolList toolList);
}
