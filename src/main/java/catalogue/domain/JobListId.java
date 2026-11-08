package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for JobList ids. */
public final class JobListId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
