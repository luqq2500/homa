package account.domain;

import java.util.UUID;

/** Type-safe generator for Provider ids. */
public final class ProviderId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
