package account.domain;

import java.util.UUID;

/** Generates the id of a data \1equest. */
public final class DataRequestId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
