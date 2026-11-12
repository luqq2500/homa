package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for Showcase ids. */
public final class ShowcaseId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
