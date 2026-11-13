package catalogue.domain;

import java.util.UUID;

/** Generates the id of a job. */
public final class JobId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
