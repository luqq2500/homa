package com.homa.verification.domain;

import java.util.Objects;

/**
 * The identity document a person verified with: its type and number (IDN-1, IDN-2). Only these two facts are held,
 * never a copy or picture of the document (IDN-2, EXT-4). Its {@code toString()} withholds the number (IDN-3).
 */
public record IdentityDocument(IdentityDocumentType type, IdentityNumber number) {

    public IdentityDocument {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(number, "number");
    }
}
