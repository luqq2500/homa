package com.homa.account.domain;

import java.util.UUID;

/** Type-safe generator for Customer ids. */
public final class CustomerId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
