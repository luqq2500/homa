package com.homa.catalogue.domain;

import java.util.UUID;

/** Type-safe generator for ToolList ids. */
public final class ToolListId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
