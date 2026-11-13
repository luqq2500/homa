package account.domain;

import java.util.UUID;

/** Generates the id of a customer \1ccount. */
public final class CustomerAccountId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
