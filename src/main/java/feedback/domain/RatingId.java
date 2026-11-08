package feedback.domain;

import java.util.UUID;

/** Type-safe generator for Rating ids. */
public final class RatingId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
