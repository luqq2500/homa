package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for Shelf ids. */
public final class ShelfId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
