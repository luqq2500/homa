package com.homa.catalogue.domain.exception;

/** Job and tool codes are upper-case words joined by underscores, such as KITCHEN_GREASE. */
public class InvalidCatalogueCodeException extends CatalogueRuleViolationException {

    public InvalidCatalogueCodeException(String code) {
        super("Not a valid catalogue code: \"" + code + "\"");
    }
}
