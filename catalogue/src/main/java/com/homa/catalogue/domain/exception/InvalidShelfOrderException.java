package com.homa.catalogue.domain.exception;

/** Shelves are ordered from 1, the first shelf on the homepage. */
public class InvalidShelfOrderException extends CatalogueRuleViolationException {

    public InvalidShelfOrderException(int order) {
        super("A shelf order starts at 1, was " + order);
    }
}
