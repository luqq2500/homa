package resolution.domain;

import java.util.UUID;

/** Type-safe generator for Case ids. */
public final class CaseId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
