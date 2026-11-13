package catalogue.domain;

import java.util.UUID;

/** Generates the id of a featured \1earch. */
public final class FeaturedSearchId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
