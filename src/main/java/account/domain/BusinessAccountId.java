package account.domain;

import java.util.UUID;

/** Generates the id of a business \1ccount. */
public final class BusinessAccountId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
