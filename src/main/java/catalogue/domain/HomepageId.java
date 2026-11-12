package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for Homepage ids. */
public final class HomepageId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
