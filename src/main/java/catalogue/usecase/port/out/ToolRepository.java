package catalogue.usecase.port.out;

import catalogue.domain.Tool;
import java.util.List;
import java.util.Optional;

public interface ToolRepository {

    void save(Tool tool);

    Optional<Tool> find(String id);

    /** All of them, in the order they were added. */
    List<Tool> all();
}
