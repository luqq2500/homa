package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for FeaturedSearch ids. */
public final class FeaturedSearchId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
