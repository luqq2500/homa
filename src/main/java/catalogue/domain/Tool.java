package catalogue.domain;

import java.util.Objects;

/**
 * A tool on Homa's tool list. A tool is identified by its code; its name can change, and it can be
 * retired so providers can no longer say they bring it. A tool is never deleted.
 *
 * <p>Tools change only through their {@link ToolList}.
 */
public final class Tool {

    private final ToolCode code;
    private String name;
    private boolean active;

    Tool(ToolCode code, String name) {
        this.code = Objects.requireNonNull(code);
        this.name = CatalogueText.requireName(name, "A tool");
        this.active = true;
    }

    boolean rename(String newName) {
        String renamed = CatalogueText.requireName(newName, "A tool");
        boolean changed = !renamed.equals(name);
        name = renamed;
        return changed;
    }

    void retire() {
        active = false;
    }

    public ToolCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public boolean isActive() {
        return active;
    }
}
