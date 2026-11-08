package catalogue.domain;

import catalogue.domain.exception.DuplicateToolCodeException;
import catalogue.domain.exception.ToolAlreadyRetiredException;
import catalogue.domain.exception.ToolNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Homa's tool list: the tools providers say they bring, so the provider
 * page can also show which tools a provider does not bring.
 *
 * <p>Codes are unique. Tools are retired, never deleted, so a code once used is never reused for a
 * different tool.
 */
public final class ToolList {

    private final String id;
    private final Map<ToolCode, Tool> tools = new LinkedHashMap<>();

    private ToolList(String id) {
        this.id = id;
    }

    public static ToolList create() {
        return new ToolList(new ToolListId().generate());
    }

    public void add(ToolCode code, String name) {
        if (tools.containsKey(code)) {
            throw new DuplicateToolCodeException(code.value());
        }
        tools.put(code, new Tool(code, name));
    }

    public void rename(ToolCode code, String name) {
        existing(code).rename(name);
    }

    public void retire(ToolCode code) {
        Tool tool = existing(code);
        if (!tool.isActive()) {
            throw new ToolAlreadyRetiredException(code.value());
        }
        tool.retire();
    }

    /** Whether providers can say they bring this tool now: it is on the list and not retired. */
    public boolean isOffered(ToolCode code) {
        return tool(code).map(Tool::isActive).orElse(false);
    }

    public boolean contains(ToolCode code) {
        return tools.containsKey(code);
    }

    public Optional<Tool> tool(ToolCode code) {
        return Optional.ofNullable(tools.get(code));
    }

    /** Every tool, retired ones included, in the order they were added. */
    public List<Tool> tools() {
        return List.copyOf(tools.values());
    }

    /** The codes providers can pick now. */
    public Set<ToolCode> offeredCodes() {
        return tools.values().stream().filter(Tool::isActive).map(Tool::code).collect(Collectors.toUnmodifiableSet());
    }

    public String id() {
        return id;
    }

    private Tool existing(ToolCode code) {
        return tool(code).orElseThrow(() -> new ToolNotFoundException(code.value()));
    }
}
