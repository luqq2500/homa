package policy.domain;

import java.util.UUID;

/** Generates the id of a value change. */
public final class ValueChangeId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
