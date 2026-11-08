package com.homa.account.domain;

import java.util.UUID;

/** Type-safe generator for Helper ids. */
public final class HelperId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
