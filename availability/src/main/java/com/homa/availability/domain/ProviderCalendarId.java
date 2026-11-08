package com.homa.availability.domain;

import java.util.UUID;

/** Type-safe generator for ProviderCalendar ids. */
public final class ProviderCalendarId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
