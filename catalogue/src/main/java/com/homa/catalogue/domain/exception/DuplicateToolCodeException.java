package com.homa.catalogue.domain.exception;

/** Tool codes are unique, including retired tools, which are never deleted. */
public class DuplicateToolCodeException extends CatalogueRuleViolationException {

    public DuplicateToolCodeException(String code) {
        super("The tool list already has a tool with code " + code);
    }
}
