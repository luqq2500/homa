package catalogue.domain;

import java.util.UUID;

/** Type-safe generator for ServiceArea ids. */
public final class ServiceAreaId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
