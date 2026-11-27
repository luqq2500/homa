package com.homa.verification.domain;

import java.util.Objects;

/**
 * A kind of identity document, named as the country calls it (in Malaysia: MyKad, MyPR, i-Kad). It is a value, not
 * an enum, so a new country brings its own documents in its {@link CountryPolicy} without changing any rule.
 */
public record IdentityDocumentType(String name) {

    public IdentityDocumentType {
        Objects.requireNonNull(name, "name");
    }
}
