package catalogue.support;

import catalogue.domain.Tool;
import catalogue.usecase.port.out.ToolRepository;
import support.InMemoryStore;

public final class InMemoryToolRepository extends InMemoryStore<Tool> implements ToolRepository {

    public InMemoryToolRepository() {
        super(Tool::id);
    }
}
