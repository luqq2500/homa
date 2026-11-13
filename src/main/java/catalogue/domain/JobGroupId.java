package catalogue.domain;

import java.util.UUID;

/** Generates the id of a job \1roup. */
public final class JobGroupId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
