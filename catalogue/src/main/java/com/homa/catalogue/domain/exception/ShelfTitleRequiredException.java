package com.homa.catalogue.domain.exception;

/** A shelf needs a title to show on the homepage. */
public class ShelfTitleRequiredException extends CatalogueRuleViolationException {

    public ShelfTitleRequiredException() {
        super("A shelf needs a title");
    }
}
