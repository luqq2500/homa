package com.homa.conversation.domain;

/** A provider, as the other contexts' events name it. */
public record ProviderId(String value) {

    public ProviderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A provider id must not be blank");
        }
    }
}
