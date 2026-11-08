package com.homa.account.domain;

/** A reference to a stored photo, such as a helper's photo shown to customers. */
public record Photo(String reference) {

    public Photo {
        reference = AccountText.require(reference, "A photo");
    }

    public static Photo of(String reference) {
        return new Photo(reference);
    }
}
