package catalogue.domain;

import java.util.UUID;

/** Generates the id of a tool. */
public final class ToolId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
